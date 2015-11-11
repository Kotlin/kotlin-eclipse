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

public class KotlinSemanticHighlightingVisitor(val editor: KotlinFileEditor) : KtVisitorVoid() {
    private val bindingContext: BindingContext
        get() = KotlinAnalysisFileCache.getAnalysisResult(editor.parsedFile!!, editor.javaProject!!).analysisResult.bindingContext
    
    private val positions = arrayListOf<HighlightPosition>()
    
    fun computeHighlightingRanges(): List<HighlightPosition> {
        positions.clear()
        editor.parsedFile!!.acceptChildren(this)
        return positions.toList() // make copy
    }
    
    private fun highlight(styleKey: String, range: TextRange) {
        val shiftedStart = LineEndUtil.convertLfToDocumentOffset(
                editor.parsedFile!!.getText(), 
                range.getStartOffset(), 
                editor.document)
        positions.add(HighlightPosition(styleKey, shiftedStart, range.getLength()))
    }
    
    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }
    
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        if (expression.getParent() is KtThisExpression) return
        
        val target = bindingContext[BindingContext.REFERENCE_TARGET, expression]
        if (target == null) return
        
        when (target) {
            is PropertyDescriptor -> highlightProperty(expression, target)
            is VariableDescriptor -> highlightVariable(expression, target)
        }
        super.visitSimpleNameExpression(expression)
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
    
    private fun visitVariableDeclaration(declaration: KtNamedDeclaration) {
        val declarationDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
        val nameIdentifier = declaration.getNameIdentifier()
        if (nameIdentifier != null && declarationDescriptor != null) {
            highlightVariable(nameIdentifier, declarationDescriptor)
        }
    }
    
    private fun highlightProperty(element: PsiElement, descriptor: PropertyDescriptor) {
        val range = element.getTextRange()
        if (DescriptorUtils.isStaticDeclaration(descriptor)) {
            if (descriptor.isVar()) {
                highlight(SemanticHighlightings.STATIC_FIELD, range)
            } else {
                highlight(SemanticHighlightings.STATIC_FINAL_FIELD, range)
            }
        } else {
            highlight(SemanticHighlightings.FIELD, range)
        }
    }
    
    private fun highlightVariable(element: PsiElement, descriptor: DeclarationDescriptor) {
        if (descriptor !is VariableDescriptor) return
        
//        val underline = descriptor.isVar()
        
        when (descriptor) {
            is LocalVariableDescriptor -> highlight(SemanticHighlightings.LOCAL_VARIABLE, element.getTextRange())
            is ValueParameterDescriptor -> highlight(SemanticHighlightings.PARAMETER_VARIABLE, element.getTextRange())
        }
    }
}

class HighlightPosition(val styleKey: String, offset: Int, length: Int) : Position(offset, length)