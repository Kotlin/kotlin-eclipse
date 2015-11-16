package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.BindingContext
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.analyzer.*
import com.intellij.psi.PsiWhiteSpace
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy
import org.jetbrains.kotlin.ui.editors.selection.handlers.siblings;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope

public class KotlinConvertToExpressionBodyAssistProposal: KotlinQuickAssistProposal() {
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val declaration = PsiTreeUtil.getParentOfType(psiElement, KtDeclarationWithBody::class.java) ?: return false
        val context = getBindingContext(declaration.getContainingKtFile()) ?: return false
        val value = calcValue(declaration, context)
        return value != null && !containsReturn(value)
    }

    override fun getDisplayString(): String {
        return "Convert to expression body"
    }

    override fun apply(document: IDocument, psiElement: PsiElement) {
    	val declaration = PsiTreeUtil.getParentOfType(psiElement, KtDeclarationWithBody::class.java)!!
        val (analysisResult, componentProvider) = getAnalysisResultWithProvider(declaration.getContainingKtFile())!!
        val context = analysisResult.bindingContext
        val value = calcValue(declaration, context)!!

        val setUnitType: Boolean = if (!declaration.hasDeclaredReturnType() && declaration is KtNamedFunction) {
            val valueType = context.getType(value)
            valueType != null && !KotlinBuiltIns.isUnit(valueType)
        } else {
        	false
        }
        
        val editor = getActiveEditor() ?: return
        
        replaceBody(declaration, value, editor)
        
        val omitType = (declaration.hasDeclaredReturnType() || setUnitType) &&
             declaration is KtCallableDeclaration

        insertAndSelectType(declaration, setUnitType, omitType, editor)   

    }
    
    private fun replaceBody(declaration: KtDeclarationWithBody, newBody: KtExpression, editor: KotlinFileEditor) {
        val body = declaration.getBodyExpression()!!
        val eqToken = KtPsiFactory(declaration).createEQ().getText()
        
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.getViewer().getDocument())
        val indent = AlignmentStrategy.computeIndent(declaration.getNode())        
        val valueText = AlignmentStrategy.alignCode(newBody.getNode(), indent, lineDelimiter)
        
        replace(body, "$eqToken $valueText")
    }
    
    private fun insertAndSelectType(declaration: KtDeclarationWithBody, setUnitType: Boolean, omitType:Boolean, editor: KotlinFileEditor) {
        val body = declaration.getBodyExpression()!!
        
        if (omitType && !setUnitType) {
            val callableDeclaration = declaration as KtCallableDeclaration
            val typeRef = callableDeclaration.getTypeReference()!!
            val colon = callableDeclaration.getColon()!!
            val range = TextRange(getStartOffset(colon, editor), getEndOffset(typeRef, editor))
            editor.selectAndReveal(range.getStartOffset(), range.getLength())
        }
        if (setUnitType) {
            val elementToPlaceTypeAfter = body.siblings(forward = false, withItself = false).
                first { it !is PsiWhiteSpace }
            val offset = getEndOffset(elementToPlaceTypeAfter, editor)
            val stringToInsert = ": ${JvmBuiltIns.Instance.getUnitType().toString()}"
            insertAfter(elementToPlaceTypeAfter, stringToInsert)
            if (omitType) {
            	editor.selectAndReveal(offset, stringToInsert.length) 
            }
        }   
    }

    private fun calcValue(declaration: KtDeclarationWithBody, context: BindingContext): KtExpression? {
        if (declaration is KtFunctionLiteral) return null
        val body = declaration.getBodyExpression()
        if (!declaration.hasBlockBody() || body !is KtBlockExpression) return null

        val statement = body.getStatements().singleOrNull() ?: return null
        when(statement) {
            is KtReturnExpression -> {
            	return statement.getReturnedExpression()
            }

            //TODO: IMO this is not good code, there should be a way to detect that KtExpression does not have value
            is KtDeclaration, is KtLoopExpression -> return null // is KtExpression but does not have value

            else  -> {
            	if (statement is KtBinaryExpression && statement.getOperationToken() == KtTokens.EQ) return null // assignment does not have value
            	val expressionType = context.getType(statement) ?: return null
            	if (!KotlinBuiltIns.isUnit(expressionType) && !KotlinBuiltIns.isNothing(expressionType)) return null
            	return statement
            }
        }
    }

    private fun containsReturn(element: PsiElement): Boolean {
        if (element is KtReturnExpression) return true
        //TODO: would be better to have some interface of declaration where return can be used
        if (element is KtNamedFunction || element is KtPropertyAccessor) return false // can happen inside

        var child = element.getFirstChild()
        while (child != null) {
            if (containsReturn(child)) return true
            child = child.getNextSibling()
        }

        return false
    }
}

fun KtCallableDeclaration.setType(type: KotlinType) {
    if (type.isError()) return
    val typeReference = KtPsiFactory(getProject()).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    setTypeReference(typeReference)
}