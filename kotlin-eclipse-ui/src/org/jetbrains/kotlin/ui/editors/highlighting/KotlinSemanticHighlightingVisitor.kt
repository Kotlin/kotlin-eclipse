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
package org.jetbrains.kotlin.ui.editors.highlighting

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.eclipse.jface.text.Position
import org.jetbrains.kotlin.psi.KtVisitorVoid
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.eclipse.jface.text.IDocument
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.descriptors.ClassKind.*
import org.jetbrains.kotlin.psi.psiUtil.getCalleeHighlightingRange
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSuperExpression

public class KotlinSemanticHighlightingVisitor(val ktFile: KtFile, val document: IDocument, val project: IJavaProject) : KtVisitorVoid() {
    private lateinit var bindingContext: BindingContext
    
    private val positions = arrayListOf<HighlightPosition>()
    
    fun computeHighlightingRanges(): List<HighlightPosition> {
        positions.clear()
        bindingContext = KotlinAnalysisFileCache.getAnalysisResult(ktFile, project).analysisResult.bindingContext
        ktFile.acceptChildren(this)
        return positions.toList() // make copy
    }
    
    private fun highlight(styleAttributes: KotlinHighlightingAttributes, range: TextRange) {
        positions.add(HighlightPosition.StyleAttributes(styleAttributes, range.offsetInDocument(ktFile, document), range.getLength()))
    }
    
    private fun highlightSmartCast(range: TextRange, typeName: String) {
        positions.add(HighlightPosition.SmartCast(typeName, range.offsetInDocument(ktFile, document), range.getLength()))
    }
    
    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }
    
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val parentExpression = expression.getParent()
        if (parentExpression is KtThisExpression || parentExpression is KtSuperExpression) return
        
        val target = bindingContext[BindingContext.REFERENCE_TARGET, expression]?.let {
            if (it is ConstructorDescriptor) it.getContainingDeclaration() else it
        }
        
        if (target == null) return
        
        val smartCast = bindingContext.get(BindingContext.SMARTCAST, expression)
        val typeName = smartCast?.let { DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(smartCast) } ?: null
        
        when (target) {
            is TypeParameterDescriptor -> highlightTypeParameter(expression)
            is ClassDescriptor -> highlightClassDescriptor(expression, target)
            is PropertyDescriptor -> highlightProperty(expression, target, typeName)
            is VariableDescriptor -> highlightVariable(expression, target, typeName)
        }
        super.visitSimpleNameExpression(expression)
    }
    
    override fun visitTypeParameter(parameter: KtTypeParameter) {
        val identifier = parameter.getNameIdentifier()
        if (identifier != null) {
            highlightTypeParameter(identifier)
        }
        
        super.visitTypeParameter(parameter)
    }
    
    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        val identifier = classOrObject.getNameIdentifier()
        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject)
        if (identifier != null && classDescriptor != null) {
            highlightClassDescriptor(identifier, classDescriptor)
        }
        
        super.visitClassOrObject(classOrObject)
    }
    
    override fun visitProperty(property: KtProperty) {
        val nameIdentifier = property.getNameIdentifier()
        if (nameIdentifier == null) return
        val propertyDescriptor = bindingContext[BindingContext.VARIABLE, property]
        if (propertyDescriptor is PropertyDescriptor) {
            highlightProperty(nameIdentifier, propertyDescriptor)
        } else {
            visitVariableDeclaration(property)
        }
        
        super.visitProperty(property)
    }
    
    override fun visitParameter(parameter: KtParameter) {
        val nameIdentifier = parameter.getNameIdentifier()
        if (nameIdentifier == null) return
        val propertyDescriptor = bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter]
        if (propertyDescriptor is PropertyDescriptor) {
            highlightProperty(nameIdentifier, propertyDescriptor)
        } else {
            visitVariableDeclaration(parameter)
        }
        
        super.visitParameter(parameter)
    }
    
    override fun visitNamedFunction(function: KtNamedFunction) {
        val nameIdentifier = function.getNameIdentifier()
        if (nameIdentifier != null) {
            highlight(KotlinHighlightingAttributes.FUNCTION_DECLARATION, nameIdentifier.getTextRange())
        }
        
        super.visitNamedFunction(function)
    }
    
    private fun visitVariableDeclaration(declaration: KtNamedDeclaration) {
        val declarationDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
        val nameIdentifier = declaration.getNameIdentifier()
        if (nameIdentifier != null && declarationDescriptor != null) {
            highlightVariable(nameIdentifier, declarationDescriptor)
        }
    }
    
    private fun highlightClassDescriptor(element: PsiElement, target: ClassDescriptor) {
        when (target.kind) {
            INTERFACE -> highlight(KotlinHighlightingAttributes.INTERFACE, element.getTextRange())
            ANNOTATION_CLASS -> {
                val range = when (element) {
                     is KtElement -> element.getCalleeHighlightingRange()
                     else -> element.getTextRange()
                }
                
                highlight(KotlinHighlightingAttributes.ANNOTATION, range)
            }
            ENUM_ENTRY -> highlight(KotlinHighlightingAttributes.STATIC_FINAL_FIELD, element.getTextRange())
            ENUM_CLASS -> highlight(KotlinHighlightingAttributes.ENUM_CLASS, element.getTextRange())
            CLASS, OBJECT -> highlight(KotlinHighlightingAttributes.CLASS, element.getTextRange())
        }
    }
    
    private fun highlightTypeParameter(element: PsiElement) {
        highlight(KotlinHighlightingAttributes.TYPE_PARAMETER, element.getTextRange())
    }
    
    private fun highlightProperty(element: PsiElement, descriptor: PropertyDescriptor, typeName: String? = null) {
        val range = element.getTextRange()
        val mutable = descriptor.isVar()
        val attributes = if (DescriptorUtils.isStaticDeclaration(descriptor)) {
            if (mutable) KotlinHighlightingAttributes.STATIC_FIELD else KotlinHighlightingAttributes.STATIC_FINAL_FIELD
        } else {
            if (mutable) KotlinHighlightingAttributes.FIELD else KotlinHighlightingAttributes.FINAL_FIELD
        }
        if (typeName != null) highlightSmartCast(element.getTextRange(), typeName)
        highlight(attributes, range)
    }
    
    private fun highlightVariable(element: PsiElement, descriptor: DeclarationDescriptor, typeName: String? = null) {
        if (descriptor !is VariableDescriptor) return
        
        val attributes = when (descriptor) {
            is LocalVariableDescriptor -> {
                if (descriptor.isVar()) {
                    KotlinHighlightingAttributes.LOCAL_VARIABLE
                } else {
                    KotlinHighlightingAttributes.LOCAL_FINAL_VARIABLE
                }
            }
            
            is ValueParameterDescriptor -> KotlinHighlightingAttributes.PARAMETER_VARIABLE
            
            else -> throw IllegalStateException("Cannot find highlight attributes for $descriptor")
        }
        if (typeName != null) highlightSmartCast(element.getTextRange(), typeName)
        highlight(attributes, element.getTextRange())
    }
}

private fun TextRange.offsetInDocument(ktFile: KtFile, document: IDocument): Int {
    return LineEndUtil.convertLfToDocumentOffset(ktFile.getText(), this.getStartOffset(), document)
}

sealed class HighlightPosition(offset: Int, length: Int) : Position(offset, length) {
    class StyleAttributes(val styleAttributes: KotlinHighlightingAttributes, offset: Int, length: Int) : HighlightPosition(offset, length)
    
    class SmartCast(val typeName: String, offset: Int, length: Int) : HighlightPosition(offset, length)
}