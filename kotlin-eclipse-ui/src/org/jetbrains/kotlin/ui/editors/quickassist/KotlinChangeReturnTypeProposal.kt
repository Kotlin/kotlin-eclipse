package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.eclipse.ui.utils.getBindingContext
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.approximateWithResolvableType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunction
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.codeassist.getResolutionScope

class KotlinChangeReturnTypeProposal(editor: KotlinEditor) : KotlinQuickAssistProposal(editor) {
    private lateinit var function: KtFunction
    private lateinit var type: KotlinType
    
    private val activeDiagnostics = listOf(
            Errors.CONSTANT_EXPECTED_TYPE_MISMATCH,
            Errors.TYPE_MISMATCH,
            Errors.NULL_FOR_NONNULL_TYPE,
            Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH)
    
    override fun apply(document: IDocument, psiElement: PsiElement) {
        val oldTypeRef = function.getTypeReference()
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)
        if (oldTypeRef != null) {
            replace(oldTypeRef, renderedType)
        } else {
            val anchor = function.getValueParameterList()
            if (anchor != null) {
                insertAfter(anchor, ": $renderedType")
            }
        }
    }
    
    override fun getDisplayString(): String {
        val functionName = function.getName()
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)
        return if (functionName != null) {
            "Change '$functionName' function return type to '$renderedType'"
        } else {
            "Change function return type to '$renderedType'"
        }
    }
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val bindingContext = getBindingContext()
        if (bindingContext == null) return false
        
        val activeDiagnostic = getActiveDiagnostic(psiElement.textOffset, bindingContext.diagnostics)
        if (activeDiagnostic == null) return false
        
        val expression = PsiTreeUtil.getNonStrictParentOfType(activeDiagnostic.psiElement, KtExpression::class.java)
        if (expression == null) return false
        
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
            
            Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH -> {
                val diagnosticWithParameters = Errors.TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.cast(activeDiagnostic)
                diagnosticWithParameters.getB()
            }
            
            else -> null
        }
        
        if (expressionType == null) return false
        
        val expressionParent = expression.getParent()
        val ktFunction = if (expressionParent is KtReturnExpression) {
            expressionParent.getTargetFunction(bindingContext)
        } else {
            PsiTreeUtil.getParentOfType(expression, KtFunction::class.java, true)
        }
        
        if (ktFunction !is KtFunction) return false
        
        return when {
            QuickFixUtil.canFunctionOrGetterReturnExpression(ktFunction, expression) -> {
                val scope = ktFunction.getResolutionScope(bindingContext)
                type = expressionType.approximateWithResolvableType(scope, false)
                function = ktFunction
                true
            }
            
            expression is KtCallExpression -> {
                type = expressionType
                function = ktFunction
                true
            }
            
            else -> false
        }
    }
    
    private fun getActiveDiagnostic(offset: Int, diagnostics: Diagnostics): Diagnostic? {
        return diagnostics.find { diagnostic ->
            if (diagnostic.textRanges.isEmpty()) return@find false
            
            val range = diagnostic.textRanges.first()
            range.startOffset <= offset && offset <= range.endOffset && diagnostic.factory in activeDiagnostics
        }
    }
    
    private fun getBindingContext(): BindingContext? {
        val ktFile = editor.parsedFile
        if (ktFile == null) return null
        
        return getBindingContext(ktFile)
    }
}