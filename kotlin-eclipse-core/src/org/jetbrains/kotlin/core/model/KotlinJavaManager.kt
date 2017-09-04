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

import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.JavaModelException
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.eclipse.jdt.core.IJavaElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.psi.KtDeclaration
import com.intellij.psi.PsiElement
import org.eclipse.jdt.core.IMethod
import org.jetbrains.kotlin.core.asJava.equalsJvmSignature
import org.jetbrains.kotlin.core.asJava.getTypeFqName
import org.eclipse.jdt.core.dom.IBinding
import org.eclipse.jdt.core.dom.IMethodBinding
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.eclipse.jdt.core.IMember
import org.jetbrains.kotlin.psi.KtProperty
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

public object KotlinJavaManager {
    @JvmField
    public val KOTLIN_BIN_FOLDER: Path = Path("kotlin_bin")
    
    public fun getKotlinBinFolderFor(project: IProject): IFolder = project.getFolder(KOTLIN_BIN_FOLDER)
    
    public fun findEclipseType(jetClass: KtClassOrObject, javaProject: IJavaProject): IType? {
        return jetClass.getFqName().let {
            if (it != null) javaProject.findType(it.asString()) else null
        }
    }
    
    public fun <T : IMember> findEclipseMembers(declaration: KtDeclaration, javaProject: IJavaProject, 
            klass: Class<T>): List<IMember> {
        val containingElement = PsiTreeUtil.getParentOfType(declaration, KtClassOrObject::class.java, KtFile::class.java)
        val seekInParent: Boolean = containingElement is KtObjectDeclaration && containingElement.isCompanion()
        
        if (containingElement == null) return emptyList()
        
        val declaringTypeFqName = getTypeFqName(containingElement)
        if (declaringTypeFqName == null) return emptyList()
        
        val eclipseType = javaProject.findType(declaringTypeFqName.asString())
        if (eclipseType == null) return emptyList()
        
        val typeMembers = findMembersIn(eclipseType, declaration, klass)
        return if (seekInParent) {
                val parentMembers = findMembersIn(eclipseType.getDeclaringType(), declaration, klass)
                typeMembers + parentMembers
            } else {
                typeMembers
            }
    }
    
    public fun hasLinkedKotlinBinFolder(project: IProject): Boolean {
        val folder = project.getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER)
        return folder.isLinked() && KotlinFileSystem.SCHEME == folder.getLocationURI().getScheme()
    }
    
    private fun <T : IMember> findMembersIn(eclipseType: IType, declaration: KtDeclaration, klass: Class<T>): List<IMember> {
        
        fun check(member: IMember): Boolean { 
            return klass.isAssignableFrom(member.javaClass) && equalsJvmSignature(declaration, member)
        }
        
        val methods = eclipseType.getMethods().filter { check(it) }
        val fields = eclipseType.getFields().filter { check(it) }
        
        return methods + fields
    }
}

public fun KtElement.toLightElements(): List<IJavaElement> {
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
        is KtProperty -> KotlinJavaManager.findEclipseMembers(this, javaProject, IMember::class.java) 
        else -> emptyList()
    }
}

public fun SourceElement.toJavaElements(): List<IJavaElement> {
    return when (this) {
        is EclipseJavaSourceElement -> obtainJavaElement(this.getElementBinding())?.let(::listOf) ?: emptyList()
        is KotlinSourceElement -> this.psi.toLightElements()
        else -> emptyList<IJavaElement>()
    }
}

public fun sourceElementsToLightElements(sourceElements: List<SourceElement>): List<IJavaElement> {
    return sourceElements.flatMap { it.toJavaElements() }
}

private fun obtainJavaElement(binding: IBinding): IJavaElement? {
    return if (binding is IMethodBinding && binding.isDefaultConstructor()) {
        binding.getDeclaringClass().getJavaElement()
    } else {
        binding.getJavaElement()
    }
}


