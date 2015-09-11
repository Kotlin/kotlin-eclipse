package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetReturnExpression
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetLoopExpression
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetPropertyAccessor
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.BindingContext
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import com.intellij.psi.PsiWhiteSpace
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy
import org.jetbrains.kotlin.ui.editors.selection.handlers.siblings;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.utils.asLexicalScope
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

public class KotlinConvertToExpressionBodyAssistProposal: KotlinQuickAssistProposal() {
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val declaration = PsiTreeUtil.getParentOfType(psiElement, JetDeclarationWithBody::class.java) ?: return false
        val context = getBindingContext(declaration.getContainingJetFile()) ?: return false
        val value = calcValue(declaration, context)
        return value != null && !containsReturn(value)
    }

    override fun getDisplayString(): String {
        return "Convert to expression body"
    }

    override fun apply(document: IDocument, psiElement: PsiElement) {
    	val declaration = PsiTreeUtil.getParentOfType(psiElement, JetDeclarationWithBody::class.java)!!
        val (analysisResult, componentProvider) = getAnalysisResultWithProvider(declaration.getContainingJetFile())!!
        val context = analysisResult.bindingContext
        val value = calcValue(declaration, context)!!

        val setUnitType: Boolean = if (!declaration.hasDeclaredReturnType() && declaration is JetNamedFunction) {
            val valueType = context.getType(value)
            valueType != null && !KotlinBuiltIns.isUnit(valueType)
        } else {
        	false
        }
        
        val editor = getActiveEditor() ?: return
        
        replaceBody(declaration, value, editor)
        
        val omitType = (declaration.hasDeclaredReturnType() || setUnitType) &&
             declaration is JetCallableDeclaration && canOmitType(declaration, value, context, componentProvider, setUnitType)

        insertAndSelectType(declaration, setUnitType, omitType, editor)   

    }
    
    private fun replaceBody(declaration: JetDeclarationWithBody, newBody: JetExpression, editor: KotlinFileEditor) {
        val body = declaration.getBodyExpression()!!
        val eqToken = JetPsiFactory(declaration).createEQ().getText()
        
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.getViewer().getDocument())
        val indent = AlignmentStrategy.computeIndent(declaration.getNode())        
        val valueText = AlignmentStrategy.alignCode(newBody.getNode(), indent, lineDelimiter)
        
        replace(body, "$eqToken $valueText")
    }
    
    private fun insertAndSelectType(declaration: JetDeclarationWithBody, setUnitType: Boolean, omitType:Boolean, editor: KotlinFileEditor) {
        val body = declaration.getBodyExpression()!!
        
        if (omitType && !setUnitType) {
            val callableDeclaration = declaration as JetCallableDeclaration
            val typeRef = callableDeclaration.getTypeReference()!!
            val colon = callableDeclaration.getColon()!!
            val range = TextRange(getStartOffset(colon, editor), getEndOffset(typeRef, editor))
            editor.selectAndReveal(range.getStartOffset(), range.getLength())
        }
        if (setUnitType) {
            val elementToPlaceTypeAfter = body.siblings(forward = false, withItself = false).
                first { it !is PsiWhiteSpace }
            val offset = getEndOffset(elementToPlaceTypeAfter, editor)
            val stringToInsert = ": ${KotlinBuiltIns.getInstance().getUnitType().toString()}"
            insertAfter(elementToPlaceTypeAfter, stringToInsert)
            if (omitType) {
            	editor.selectAndReveal(offset, stringToInsert.length()) 
            }
        }   
    }

    private fun calcValue(declaration: JetDeclarationWithBody, context: BindingContext): JetExpression? {
        if (declaration is JetFunctionLiteral) return null
        val body = declaration.getBodyExpression()
        if (!declaration.hasBlockBody() || body !is JetBlockExpression) return null

        val statement = body.getStatements().singleOrNull() ?: return null
        when(statement) {
            is JetReturnExpression -> {
            	return statement.getReturnedExpression()
            }

            //TODO: IMO this is not good code, there should be a way to detect that JetExpression does not have value
            is JetDeclaration, is JetLoopExpression -> return null // is JetExpression but does not have value

            else  -> {
            	if (statement is JetBinaryExpression && statement.getOperationToken() == JetTokens.EQ) return null // assignment does not have value
            	val expressionType = context.getType(statement) ?: return null
            	if (!KotlinBuiltIns.isUnit(expressionType) && !KotlinBuiltIns.isNothing(expressionType)) return null
            	return statement
            }
        }
    }

    private fun containsReturn(element: PsiElement): Boolean {
        if (element is JetReturnExpression) return true
        //TODO: would be better to have some interface of declaration where return can be used
        if (element is JetNamedFunction || element is JetPropertyAccessor) return false // can happen inside

        var child = element.getFirstChild()
        while (child != null) {
            if (containsReturn(child)) return true
            child = child.getNextSibling()
        }

        return false
    }
    
    private fun canOmitType(declaration: JetCallableDeclaration, expression: JetExpression, 
            bindingContext: BindingContext, provider: ComponentProvider, setUnitType: Boolean): Boolean {
        if (!declaration.canRemoveTypeSpecificationByVisibility(bindingContext))
            return false

        // Workaround for anonymous objects and similar expressions without resolution scope
        // TODO: This should probably be fixed in front-end so that resolution scope is recorded for anonymous objects as well
        val scopeExpression = ((declaration as? JetDeclarationWithBody)?.getBodyExpression() as? JetBlockExpression)
                                 ?.getStatements()?.singleOrNull()
                         ?: return false
        
        val declaredType: JetType = if (setUnitType) {
            KotlinBuiltIns.getInstance().getUnitType()
        } else {
            (bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? CallableDescriptor)?.getReturnType() ?: return false
        }
        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, scopeExpression] ?: return false
        val expressionType = expression.computeTypeInContext(provider, scope)
        return expressionType?.isSubtypeOf(declaredType) ?: false
    }
}

private fun JetExpression.computeTypeInContext(provider: ComponentProvider, scope: JetScope): JetType? {
    return provider.getService(ExpressionTypingServices::class.java).getTypeInfo(
            scope.asLexicalScope(), 
            this,
            TypeUtils.NO_EXPECTED_TYPE,
            DataFlowInfo.EMPTY,
            BindingTraceContext(),
            false).type
}

fun JetCallableDeclaration.setType(type: JetType) {
    if (type.isError()) return
    val typeReference = JetPsiFactory(getProject()).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    setTypeReference(typeReference)
}