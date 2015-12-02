package org.jetbrains.kotlin.ui.editors.quickassist

import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.ui.editors.codeassist.getResolutionScope
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class KotlinChangeReturnTypeFixGenerator : KotlinQuickAssistProposalsGenerator() {
    override fun getProposals(editor: KotlinFileEditor, psiElement: PsiElement): List<KotlinQuickAssistProposal> {
        val expression = PsiTreeUtil.getNonStrictParentOfType(psiElement, KtExpression::class.java)
        if (expression == null) return emptyList()
        
        val bindingContext = getBindingContext(editor)
        if (bindingContext == null) return emptyList()
        
        val activeDiagnostic = getActiveDiagnostic(psiElement.textOffset, bindingContext.diagnostics)
        if (activeDiagnostic == null) return emptyList()
        
        val expectedType: KotlinType?
        val expressionType: KotlinType?
        when (activeDiagnostic.factory) {
            Errors.TYPE_MISMATCH -> {
                val diagnosticWithParameters = Errors.TYPE_MISMATCH.cast(activeDiagnostic)
                expectedType = diagnosticWithParameters.getA()
                expressionType = diagnosticWithParameters.getB()
            }
            
            Errors.NULL_FOR_NONNULL_TYPE -> {
                val diagnosticWithParameters = Errors.NULL_FOR_NONNULL_TYPE.cast(activeDiagnostic)
                expectedType = diagnosticWithParameters.getA()
                expressionType = expectedType.makeNullable()
            }
            
            Errors.CONSTANT_EXPECTED_TYPE_MISMATCH -> {
                val diagnosticWithParameters = Errors.CONSTANT_EXPECTED_TYPE_MISMATCH.cast(activeDiagnostic)
                expectedType = diagnosticWithParameters.getB()
                expressionType = bindingContext.getType(expression)
            }
            
            else -> return emptyList()
        }
        
        if (expectedType == null || expressionType == null) return emptyList()
        
        val expressionParent = expression.getParent()
        val function = if (expressionParent is KtReturnExpression) {
            expressionParent.getTargetFunction(bindingContext)
        } else {
            PsiTreeUtil.getParentOfType(expression, KtFunction::class.java, true)
        } 
        
        if (function is KtFunction) {
            val scope = function.getResolutionScope(bindingContext)
            val typeToInsert = expressionType.approximateWithResolvableType(scope, false)
            return listOf(KotlinChangeReturnTypeProposal(function, typeToInsert))
        }
        
        return emptyList()
    }
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        return true;
    }
    
    private fun getActiveDiagnostic(offset: Int, diagnostics: Diagnostics): Diagnostic? {
        return diagnostics.find { diagnostic ->
            if (diagnostic.textRanges.isEmpty()) return@find false
            
            val range = diagnostic.textRanges.first()
            if (range.startOffset <= offset && offset <= range.endOffset) {
                return@find true
            }
            
            true
        }
    }
    
    private fun getBindingContext(editor: KotlinFileEditor): BindingContext? {
        val ktFile = editor.parsedFile
        if (ktFile == null) return null
        
        return getBindingContext(ktFile)
    }
}