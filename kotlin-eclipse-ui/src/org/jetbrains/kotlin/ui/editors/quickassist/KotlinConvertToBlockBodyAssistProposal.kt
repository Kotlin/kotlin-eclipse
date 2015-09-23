package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.psi.JetPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.JetReturnExpression
import org.jetbrains.kotlin.psi.JetBlockExpression
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor

class KotlinConvertToBlockBodyAssistProposal: KotlinQuickAssistProposal() {
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val declaration = PsiTreeUtil.getParentOfType(psiElement, JetDeclarationWithBody::class.java)?: return false
        if (declaration is JetFunctionLiteral || declaration.hasBlockBody() || !declaration.hasBody()) return false

        when (declaration) {
            is JetNamedFunction -> {
            val bindingContext = getBindingContext(declaration.getContainingJetFile()) ?: return false;
            val returnType: JetType = declaration.returnType(bindingContext) ?: return false
            if (!declaration.hasDeclaredReturnType() && returnType.isError()) return false// do not convert when type is implicit and unknown
            return true
        }

            is JetPropertyAccessor -> return true

            else -> error("Unknown declaration type: $declaration")
        }
    }

    override fun getDisplayString() = "Convert to block body"

    override fun apply(document: IDocument, psiElement: PsiElement) {
        val declaration = PsiTreeUtil.getParentOfType(psiElement, JetDeclarationWithBody::class.java)!!
        val context = getBindingContext(declaration.getContainingJetFile())!!

        val shouldSpecifyType = declaration is JetNamedFunction 
            && !declaration.hasDeclaredReturnType() 
            && !KotlinBuiltIns.isUnit(declaration.returnType(context)!!)

        val editor = getActiveEditor() ?: return
        val factory = JetPsiFactory(declaration)

        replaceBody(declaration, factory, context, editor)

        if (shouldSpecifyType) {
        	specifyType(declaration, factory, context)
        }
    }

    private fun replaceBody(declaration: JetDeclarationWithBody, factory: JetPsiFactory, context: BindingContext, editor: KotlinFileEditor) {
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.getViewer().getDocument())
        val indent = AlignmentStrategy.computeIndent(declaration.getNode())
        
        val newBody = convert(declaration, context)
        var newBodyText = AlignmentStrategy.alignCode(newBody.getNode(), indent, lineDelimiter)

        if (declaration.getEqualsToken()!!.getNextSibling() !is PsiWhiteSpace) {
            newBodyText = factory.createWhiteSpace().getText() + newBodyText
        }

        replaceBetween(declaration.getEqualsToken()!!, declaration.getBodyExpression()!!, newBodyText)
    }
    
    private fun specifyType(declaration: JetDeclarationWithBody, factory: JetPsiFactory, context: BindingContext) {
    	val returnType = (declaration as JetNamedFunction).returnType(context).toString()
        val stringToInsert = listOf(factory.createColon(), factory.createWhiteSpace())
            .joinToString(separator = "") { it.getText()} + returnType
        insertAfter(declaration.getValueParameterList()!!, stringToInsert)
    }

    private fun convert(declaration: JetDeclarationWithBody, bindingContext: BindingContext): JetExpression {
        val body = declaration.getBodyExpression()!!

        fun generateBody(returnsValue: Boolean): JetExpression {
            val bodyType = bindingContext.getType(body)
            val needReturn = returnsValue &&
                (bodyType == null || (!KotlinBuiltIns.isUnit(bodyType) && !KotlinBuiltIns.isNothing(bodyType)))

            val factory = JetPsiFactory(declaration)
            val expression = factory.createExpression(body.getText())
            val block: JetBlockExpression = if (needReturn) {
                    factory.createBlock("return xyz")
                }
                    else {
                    return factory.createBlock(expression.getText())
                }
            val returnExpression = PsiTreeUtil.getChildOfType(block, JetReturnExpression::class.java)
            val returned = returnExpression?.getReturnedExpression()?: return factory.createBlock("return ${expression.getText()}")
            if (JetPsiUtil.areParenthesesNecessary(expression, returned, returnExpression!!)) {
                return factory.createBlock("return (${expression.getText()})")
            }
            return factory.createBlock("return ${expression.getText()}")
        }

        val newBody = when (declaration) {
                is JetNamedFunction -> {
                val returnType = declaration.returnType(bindingContext)!!
                generateBody(!KotlinBuiltIns.isUnit(returnType) && !KotlinBuiltIns.isNothing(returnType))
            }

                is JetPropertyAccessor -> generateBody(declaration.isGetter())

                else -> throw RuntimeException("Unknown declaration type: $declaration")
            }
        return newBody
    }

    private fun JetNamedFunction.returnType(context: BindingContext): JetType? {
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] ?: return null
        return (descriptor as FunctionDescriptor).getReturnType()
    }
}