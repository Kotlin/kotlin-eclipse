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
package org.jetbrains.kotlin.ui.editors.quickassist

import org.eclipse.jface.text.IDocument
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.psi.KtExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.ui.editors.codeassist.getResolutionScope
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers

public class KotlinChangeReturnTypeQuickFix : KotlinQuickAssistProposal() {
    private val activeDiagnostics = listOf(
            Errors.CONSTANT_EXPECTED_TYPE_MISMATCH,
            Errors.TYPE_MISMATCH,
            Errors.NULL_FOR_NONNULL_TYPE)
    
    private lateinit var activeDiagnostic: Diagnostic
    
    override fun apply(document: IDocument, psiElement: PsiElement) {
//        PsiTreeUtil.getNonStrictParentOfType(psiElement, KtReturnExpression::class.java)
        val expression = PsiTreeUtil.getNonStrictParentOfType(psiElement, KtExpression::class.java)
        if (expression == null) return
        
        val bindingContext = getBindingContext()
        if (bindingContext == null) return
        
        val expressionType = when (activeDiagnostic.factory) {
            Errors.TYPE_MISMATCH -> {
                val diagnosticWithParameters = Errors.TYPE_MISMATCH.cast(activeDiagnostic)
                diagnosticWithParameters.getB()
            }
            
            Errors.NULL_FOR_NONNULL_TYPE -> {
                val diagnosticWithParameters = Errors.NULL_FOR_NONNULL_TYPE.cast(activeDiagnostic)
                val expectedType = diagnosticWithParameters.getA()
                expectedType.makeNullable()
            }
            
            Errors.CONSTANT_EXPECTED_TYPE_MISMATCH -> bindingContext.getType(expression)
            
            else -> null
        }
        
        if (expressionType == null) return
        
        val action = createActions(expression, expressionType, bindingContext)
        if (action == null) return
        
        changeReturnType(action.first, action.second)
    }
    
    override fun getDisplayString(): String {
        return "Change return type"
    }
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        return isDiagnosticActiveForElement(psiElement.getTextOffset())
    }
    
    private fun changeReturnType(function: KtFunction, type: KotlinType) {
        val oldTypeRef = function.getTypeReference()
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        if (oldTypeRef != null) {
            if (KotlinBuiltIns.isUnit(type)) {
                replace(oldTypeRef, "")
            } else {
                replace(oldTypeRef, renderedType)
            }
        } else {
            val anchor = function.getValueParameterList()
            if (anchor != null) {
                insertAfter(anchor, ": $renderedType")
            }
        }
    }
    
    private fun isDiagnosticActiveForElement(offset: Int): Boolean {
        val bindingContext = getBindingContext()
        if (bindingContext == null) return false
        
        for (diagnostic in bindingContext.diagnostics) {
            if (diagnostic.getTextRanges().isEmpty()) continue
            
            val range = diagnostic.textRanges.first()
            if (range.startOffset <= offset && offset <= range.endOffset) {
                if (diagnostic.factory in activeDiagnostics) {
                    activeDiagnostic = diagnostic
                    return true
                }
            }
        }
        
        return false
    }
    
    private fun getBindingContext(): BindingContext? {
        val editor = getActiveEditor()
        if (editor == null) return null
        
        val ktFile = editor.parsedFile
        if (ktFile == null) return null
        
        return getBindingContext(ktFile)
    }
    
    private fun createActions(expression: KtExpression, expressionType: KotlinType, bindingContext: BindingContext)
        : Pair<KtFunction, KotlinType>? {
        val expressionParent = expression.getParent()
        val function = if (expressionParent is KtReturnExpression) {
            expressionParent.getTargetFunction(bindingContext)
        } else PsiTreeUtil.getParentOfType(expression, KtFunction::class.java, true)
        
        if (function != null) {
            val scope = function.getResolutionScope(bindingContext)
            val typeToInsert = expressionType.approximateWithResolvableType(scope, true)
            return Pair(function as KtFunction, typeToInsert)
        }
        
        return null
    }
}