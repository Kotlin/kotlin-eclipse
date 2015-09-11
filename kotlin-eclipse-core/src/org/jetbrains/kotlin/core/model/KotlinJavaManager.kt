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
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.eclipse.jdt.core.IJavaElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.psi.JetDeclaration
import com.intellij.psi.PsiElement
import org.eclipse.jdt.core.IMethod
import org.jetbrains.kotlin.core.asJava.equalsJvmSignature
import org.jetbrains.kotlin.core.asJava.getTypeFqName
import org.eclipse.jdt.core.dom.IBinding
import org.eclipse.jdt.core.dom.IMethodBinding
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetSecondaryConstructor
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetPropertyAccessor
import org.eclipse.jdt.core.IMember
import org.jetbrains.kotlin.psi.JetProperty
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.JetFile

public object KotlinJavaManager {
    public val KOTLIN_BIN_FOLDER: Path = Path("kotlin_bin")
    
    public fun getKotlinBinFolderFor(project: IProject): IFolder = project.getFolder(KOTLIN_BIN_FOLDER)
    
    public fun findEclipseType(jetClass: JetClassOrObject, javaProject: IJavaProject): IType? {
        return jetClass.getFqName().let {
            if (it != null) javaProject.findType(it.asString()) else null
        }
    }
    
    public fun <T : IMember> findEclipseMembers(declaration: JetDeclaration, javaProject: IJavaProject, 
            klass: Class<T>): List<IMember> {
        val containingElement = PsiTreeUtil.getParentOfType(declaration, JetClassOrObject::class.java, JetFile::class.java)
        val seekInParent: Boolean = containingElement is JetObjectDeclaration && containingElement.isCompanion()
        
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
    
    public fun hasLinkedKotlinBinFolder(javaProject: IJavaProject): Boolean {
        val folder = javaProject.getProject().getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER)
        return folder.isLinked() && KotlinFileSystem.SCHEME == folder.getLocationURI().getScheme()
    }
    
    private fun <T : IMember> findMembersIn(eclipseType: IType, declaration: JetDeclaration, klass: Class<T>): List<IMember> {
        
        fun check(member: IMember): Boolean { 
            return klass.isAssignableFrom(member.javaClass) && equalsJvmSignature(declaration, member)
        }
        
        val methods = eclipseType.getMethods().filter { check(it) }
        val fields = eclipseType.getFields().filter { check(it) }
        
        return methods + fields
    }
}

public fun JetElement.toLightElements(javaProject: IJavaProject): List<IJavaElement> {
    return when (this) {
        is JetClassOrObject -> KotlinJavaManager.findEclipseType(this, javaProject).singletonOrEmptyList()
        is JetNamedFunction,
        is JetSecondaryConstructor,
        is JetPropertyAccessor -> KotlinJavaManager.findEclipseMembers(this as JetDeclaration, javaProject, IMethod::class.java)
        is JetProperty -> KotlinJavaManager.findEclipseMembers(this, javaProject, IMember::class.java) 
        else -> emptyList()
    }
}

public fun sourceElementsToLightElements(sourceElements: List<SourceElement>, javaProject: IJavaProject): List<IJavaElement> {
    return sourceElements
            .flatMap {
                when (it) {
                    is EclipseJavaSourceElement -> obtainJavaElement(it.getElementBinding()).singletonOrEmptyList()
                    is KotlinSourceElement -> it.psi.toLightElements(javaProject)
                    else -> emptyList<IJavaElement>()
                }
            }
}

private fun obtainJavaElement(binding: IBinding): IJavaElement? {
    return if (binding is IMethodBinding && binding.isDefaultConstructor()) {
        binding.getDeclaringClass().getJavaElement()
    } else {
        binding.getJavaElement()
    }
}


