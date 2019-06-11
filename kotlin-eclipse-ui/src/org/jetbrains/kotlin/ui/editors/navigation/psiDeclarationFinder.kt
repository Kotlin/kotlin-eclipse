/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors.navigation

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.text.Document
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.DescriptorUtils


private class KotlinSearchDeclarationVisitor(private val descriptor: DeclarationDescriptor): KtVisitor<KtNamedDeclaration?, Void?>() {
    override fun visitKtFile(file: KtFile, data: Void?): KtNamedDeclaration? = 
            visitKtElement(file, data)
    
    override fun visitKtElement(element: KtElement, data: Void?): KtNamedDeclaration? =
            element.getChildren().asSequence().map { 
                when(it) {
                    is KtElement -> it.accept(this, data)
                    else -> null
                }
             }.filterNotNull().firstOrNull()
    
    override fun visitNamedDeclaration(declaration: KtNamedDeclaration, data: Void?): KtNamedDeclaration? {
        if (compareDeclarations(declaration, descriptor)) {
            return declaration
        } else {
            return super.visitNamedDeclaration(declaration, data)
        }
    }
    
    override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Void?): KtNamedDeclaration? {
        if (classOrObject.getPrimaryConstructor() == null && isConstructorForClass(classOrObject, descriptor)) {
            return classOrObject
        } else {
            return super.visitClassOrObject(classOrObject, data)
        }
    }
    
    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: Void?): KtNamedDeclaration? {
        if (compareConstructors(constructor, descriptor)) {
            return constructor
        }
        return null
    }
    
    private fun isConstructorForClass(classOrObject: KtClassOrObject, possibleConstructor: DeclarationDescriptor) = 
            possibleConstructor is ConstructorDescriptor &&
            areFqNamesEqual(possibleConstructor.getContainingDeclaration(), classOrObject)
    
    private fun compareConstructors(declaration: KtPrimaryConstructor, descriptor: DeclarationDescriptor) =
            isConstructorForClass(declaration.getContainingClassOrObject(), descriptor)
    
    private fun compareDeclarations(declaration: KtNamedDeclaration, descriptor: DeclarationDescriptor): Boolean {
        if (!areFqNamesEqual(descriptor, declaration)) {
            return false
        }
        if (descriptor is CallableDescriptor) {
            if (declaration is KtCallableDeclaration) {
                return descriptor.getValueParameters().size == declaration.getValueParameters().size
            } else {
                return false
            }
        } else {
            return true
        }
    }
    
    private fun areFqNamesEqual(descriptor: DeclarationDescriptor, declaration: KtNamedDeclaration) = 
            DescriptorUtils.getFqName(descriptor) == declaration.getFqName()?.toUnsafe()
}

fun findDeclarationInParsedFile(descriptor: DeclarationDescriptor, parsedFile: KtFile): KtNamedDeclaration? {
    val visitor = KotlinSearchDeclarationVisitor(descriptor)
    return parsedFile.accept(visitor, null)
}

fun findDeclarationInParsedFileOffset(descriptor: DeclarationDescriptor, parsedFile: KtFile): Int =
    findDeclarationInParsedFile(descriptor, parsedFile)?.textOffset ?: 0

fun findDeclarationInJavaFile(javaElement: JavaElement, javaSource: String): Int? {
    val navigationElement = if (javaElement is JavaElementImpl<*>) javaElement.psi else null
    if (navigationElement == null) return null
    
    val sourcePsi = PsiFileFactory.getInstance(navigationElement.project)
            .createFileFromText("dummy.java", JavaLanguage.INSTANCE, javaSource)
    
    val offset = when (navigationElement) {
        is PsiClass -> {
            val classes = PsiTreeUtil.findChildrenOfType(sourcePsi, PsiClass::class.java)
            val sourceElement = classes.find { it.qualifiedName == navigationElement.qualifiedName } ?: return null
            sourceElement.getTextOffset()
        }
        
        is PsiMethod -> {
            val methods = PsiTreeUtil.findChildrenOfType(sourcePsi, PsiMethod::class.java)
            val sourceElement = methods.find {
                it.name == navigationElement.name &&
                it.parameterList.parametersCount == navigationElement.parameterList.parametersCount
            } ?: return null
            
            sourceElement.getTextOffset()
        }
        
        else -> null
    }
    
    return if (offset != null) {
        return LineEndUtil.convertLfToDocumentOffset(sourcePsi.text, offset, Document(javaSource))
    } else {
        null
    }
}