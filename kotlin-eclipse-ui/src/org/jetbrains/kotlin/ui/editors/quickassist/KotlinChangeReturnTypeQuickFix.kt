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

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.psi.KtExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.ui.editors.codeassist.getResolutionScope
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.types.typeUtil.makeNullable

public class KotlinChangeReturnTypeQuickFix : KotlinQuickAssistProposal() {
    override fun apply(document: IDocument, psiElement: PsiElement) {
        val expression = PsiTreeUtil.getNonStrictParentOfType(psiElement, KtExpression::class.java)
        if (expression == null) return
        
        val editor = getActiveEditor()
        if (editor == null) return
        
        val ktFile = editor.parsedFile
        val javaProject = editor.javaProject
        
        if (ktFile == null || javaProject == null) return
        
        val annotation = DiagnosticAnnotationUtil.INSTANCE.getAnnotationByOffset(editor, getStartOffset(psiElement, editor))
        val diagnostic = annotation?.diagnostic
        if (diagnostic == null) return
        
        val analysisResult = KotlinAnalysisFileCache.getAnalysisResult(ktFile, javaProject).analysisResult
        val expressionType = when (diagnostic.factory) {
            Errors.TYPE_MISMATCH -> {
                val diagnosticWithParameters = Errors.TYPE_MISMATCH.cast(diagnostic)
                diagnosticWithParameters.getB()
            }
            
            Errors.NULL_FOR_NONNULL_TYPE -> {
                val diagnosticWithParameters = Errors.NULL_FOR_NONNULL_TYPE.cast(diagnostic)
                val expectedType = diagnosticWithParameters.getA()
                expectedType.makeNullable()
            }
            
            Errors.CONSTANT_EXPECTED_TYPE_MISMATCH -> analysisResult.bindingContext.getType(expression)
            
            else -> null
        }
        
        if (expressionType == null) return
        
        val action = createActions(expression, expressionType, analysisResult.bindingContext)
        if (action == null) return
        
        changeReturnType(action.first, action.second)
    }
    
    override fun getDisplayString(): String = "Change return type"
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val editor = getActiveEditor()
        if (editor == null) return false
        
        return isDiagnosticAnnotationActiveForElement(
                editor, 
                Errors.CONSTANT_EXPECTED_TYPE_MISMATCH,
                Errors.TYPE_MISMATCH,
                Errors.NULL_FOR_NONNULL_TYPE)
    }
    
    private fun changeReturnType(function: KtFunction, type: KotlinType) {
        val oldTypeRef = function.getTypeReference()
        if (oldTypeRef != null) {
            if (KotlinBuiltIns.isUnit(type) || !function.hasBlockBody()) {
                replace(oldTypeRef, "")
            }
            replace(oldTypeRef, IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type))
        }
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