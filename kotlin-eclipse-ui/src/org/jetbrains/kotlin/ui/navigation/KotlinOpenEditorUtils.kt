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
package org.jetbrains.kotlin.ui.navigation

import org.jetbrains.kotlin.psi.KtVisitorVoid
import com.intellij.psi.PsiElement
import org.eclipse.jdt.core.IJavaElement
import org.jetbrains.kotlin.psi.KtElement
import org.eclipse.jdt.core.IType
import org.jetbrains.kotlin.psi.KtClass
import org.eclipse.jdt.core.IMethod
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtFile
import kotlin.MutableList
import java.util.ArrayList
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.name.FqName
import org.eclipse.jdt.core.IField
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.core.asJava.LightClassBuilderFactory
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.eclipse.jdt.core.IMember
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.core.asJava.equalsJvmSignature
import org.jetbrains.kotlin.core.asJava.getDeclaringTypeFqName
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.ui.dialogs.ListDialog
import org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider
import org.eclipse.compare.internal.ListContentProvider
import org.eclipse.jface.window.Window
import org.eclipse.ui.PlatformUI

fun chooseSourceFile(sourceFiles: List<KtFile>): IFile? {
    return when {
        sourceFiles.isEmpty() -> null
        sourceFiles.size() == 1 -> KotlinPsiManager.getEclispeFile(sourceFiles.first())
        else -> chooseFile(sourceFiles)
    }
}

fun chooseFile(jetFiles: List<KtFile>): IFile? {
    val eclipseFiles = jetFiles.map(KotlinPsiManager::getEclispeFile)
    
    val dialog = ListDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell())
    dialog.setBlockOnOpen(true)
    dialog.setMessage("Select a Kotlin file to navigate")
    dialog.setTitle("Choose Kotlin file")
    dialog.setContentProvider(ListContentProvider())
    dialog.setLabelProvider(JavaUILabelProvider())
    
    dialog.setInput(eclipseFiles)
    
    if (dialog.open() != Window.OK) {
        return null
    }
    
    return dialog.getResult().firstOrNull() as? IFile
}

fun findNavigationFileFromSources(element: IJavaElement, sourceFiles: List<KtFile>): KtFile? {
    return sourceFiles.firstOrNull { findKotlinDeclaration(element, it) != null }
}

fun findKotlinDeclaration(element: IJavaElement, jetFile: KtFile): KtElement? {
    val result = ArrayList<KtElement>()
    val visitor = makeVisitor(element, result)
    if (visitor != null) {
        jetFile.acceptChildren(visitor)
    }

    return result.firstOrNull()
}

fun makeVisitor(element: IJavaElement, result: MutableList<KtElement>): KtVisitorVoid? {
    return when (element) {
        is IType -> object : JetAllVisitor() {
            override fun visitClassOrObject(jetClassOrObject: KtClassOrObject) {
                if (jetClassOrObject.getFqName() == element.getFqName()) {
                    result.add(jetClassOrObject)
                    return
                }

                jetClassOrObject.acceptChildren(this)
            }
        }
        
        is IField -> object: JetAllVisitor() {
            override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
                visitExplicitDeclaration(declaration)
                declaration.acceptChildren(this)
            }

            override fun visitEnumEntry(enumEntry: KtEnumEntry) {
                visitExplicitDeclaration(enumEntry)
            }

            override fun visitProperty(property: KtProperty) {
                visitExplicitDeclaration(property)
            }

            fun visitExplicitDeclaration(declaration: KtDeclaration) {
                if (equalsJvmSignature(declaration, element)) {
                    result.add(declaration)
                }
            }
        }
        
        is IMethod -> object : JetAllVisitor() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                visitExplicitDeclaration(function)
            }

            override fun visitProperty(property: KtProperty) {
                visitExplicitDeclaration(property)
                property.acceptChildren(this)
            }

            override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
                visitExplicitDeclaration(accessor)
            }

            override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
                visitExplicitDeclaration(constructor)
            }

            override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
                visitExplicitDeclaration(constructor)
            }

//          Check primary constructor when there are no secondary constructors
            override fun visitClass(jetClass: KtClass) {
                if (equalsJvmSignature(jetClass, element) && (jetClass.getFqName() == element.getDeclaringType().getFqName())) {
                    result.add(jetClass)
                    return
                }

                jetClass.acceptChildren(this)
            }

            fun visitExplicitDeclaration(declaration: KtDeclaration) {
                if (equalsJvmSignature(declaration, element) && equalsDeclaringTypes(declaration, element)) {
                    result.add(declaration)
                }
            }
        }
        else -> null
    }
}

fun IType.getFqName(): FqName {
    return FqName(this.getFullyQualifiedName('.'))
}

fun equalsDeclaringTypes(jetElement: KtElement, javaMember: IMember): Boolean  {
    val typeNameInfo = getDeclaringTypeFqName(jetElement)
    val javaTypeFqName = javaMember.getDeclaringType().getFqName() 
    return typeNameInfo.className == javaTypeFqName || typeNameInfo.filePartName == javaTypeFqName
}

open class JetAllVisitor() : KtVisitorVoid() {
    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }
}