/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.model

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.*
import org.eclipse.jdt.core.dom.IBinding
import org.eclipse.jdt.core.dom.IMethodBinding
import org.jetbrains.kotlin.core.asJava.equalsJvmSignature
import org.jetbrains.kotlin.core.asJava.getTypeFqName
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

object KotlinJavaManager {
    @JvmField
    val KOTLIN_BIN_FOLDER: Path = Path("kotlin_bin")
    
    fun getKotlinBinFolderFor(project: IProject): IFolder = project.getFolder(KOTLIN_BIN_FOLDER)
    
    fun findEclipseType(jetClass: KtClassOrObject, javaProject: IJavaProject): IType? {
        return jetClass.fqName.let {
            if (it != null) javaProject.findType(it.asString()) else null
        }
    }
    
    fun <T : IMember> findEclipseMembers(declaration: KtDeclaration, javaProject: IJavaProject,
            klass: Class<T>): List<IMember> {
        val containingElement = PsiTreeUtil.getParentOfType(declaration, KtClassOrObject::class.java, KtFile::class.java)
        val seekInParent: Boolean = containingElement is KtObjectDeclaration && containingElement.isCompanion()
        
        if (containingElement == null) return emptyList()
        
        val declaringTypeFqName = getTypeFqName(containingElement) ?: return emptyList()

        val eclipseType = javaProject.findType(declaringTypeFqName.asString()) ?: return emptyList()

        val typeMembers = findMembersIn(eclipseType, declaration, klass)
        return if (seekInParent) {
                val parentMembers = findMembersIn(eclipseType.declaringType, declaration, klass)
                typeMembers + parentMembers
            } else {
                typeMembers
            }
    }
    
    fun hasLinkedKotlinBinFolder(project: IProject): Boolean {
        val folder = project.getFolder(KOTLIN_BIN_FOLDER)
        return folder.isLinked && KotlinFileSystem.SCHEME == folder.locationURI.scheme
    }
    
    private fun <T : IMember> findMembersIn(eclipseType: IType, declaration: KtDeclaration, klass: Class<T>): List<IMember> {
        
        fun check(member: IMember): Boolean { 
            return klass.isAssignableFrom(member.javaClass) && equalsJvmSignature(declaration, member)
        }
        
        val methods = eclipseType.methods.filter { check(it) }
        val fields = eclipseType.fields.filter { check(it) }
        
        return methods + fields
    }
}

fun KtElement.toLightElements(): List<IJavaElement> {
    val javaProject = KotlinPsiManager.getJavaProject(this)
    if (javaProject == null) {
        KotlinLogger.logWarning("Cannot resolve jetElement ($this) to light elements: there is no corresponding java project")
        return emptyList()
    }
    
    if (!javaProject.isOpen) {
        return emptyList()
    }
    
    return when (this) {
        is KtClassOrObject -> KotlinJavaManager.findEclipseType(this, javaProject)?.let(::listOf) ?: emptyList()
        is KtNamedFunction,
        is KtSecondaryConstructor,
        is KtPrimaryConstructor,
        is KtPropertyAccessor -> KotlinJavaManager.findEclipseMembers(this as KtDeclaration, javaProject, IMethod::class.java)
        is KtProperty -> {
            val list = KotlinJavaManager.findEclipseMembers(this, javaProject, IMember::class.java)
            val getterList = getter?.toLightElements() ?: emptyList()
            val setterList = setter?.toLightElements() ?: emptyList()
            list + getterList + setterList
        }
        else -> emptyList()
    }
}

fun SourceElement.toJavaElements(): List<IJavaElement> {
    return when (this) {
        is EclipseJavaSourceElement -> obtainJavaElement(elementBinding)?.let(::listOf) ?: emptyList()
        is KotlinSourceElement -> psi.toLightElements()
        else -> emptyList()
    }
}

fun sourceElementsToLightElements(sourceElements: List<SourceElement>): List<IJavaElement> {
    return sourceElements.flatMap { it.toJavaElements() }
}

private fun obtainJavaElement(binding: IBinding): IJavaElement? {
    return if (binding is IMethodBinding && binding.isDefaultConstructor) {
        binding.declaringClass.javaElement
    } else {
        binding.javaElement
    }
}


