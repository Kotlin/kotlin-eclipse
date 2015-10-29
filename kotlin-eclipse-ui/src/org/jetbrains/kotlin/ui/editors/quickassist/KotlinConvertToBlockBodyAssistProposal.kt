package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.core.model.KotlinEnvironment

class KotlinConvertToBlockBodyAssistProposal: KotlinQuickAssistProposal() {
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val declaration = PsiTreeUtil.getParentOfType(psiElement, KtDeclarationWithBody::class.java)?: return false
        if (declaration is KtFunctionLiteral || declaration.hasBlockBody() || !declaration.hasBody()) return false

        when (declaration) {
            is KtNamedFunction -> {
            val bindingContext = getBindingContext(declaration.getContainingJetFile()) ?: return false;
            val returnType: KotlinType = declaration.returnType(bindingContext) ?: return false
            if (!declaration.hasDeclaredReturnType() && returnType.isError()) return false// do not convert when type is implicit and unknown
            return true
        }

            is KtPropertyAccessor -> return true

            else -> error("Unknown declaration type: $declaration")
        }
    }

    override fun getDisplayString() = "Convert to block body"

    override fun apply(document: IDocument, psiElement: PsiElement) {
        val declaration = PsiTreeUtil.getParentOfType(psiElement, KtDeclarationWithBody::class.java)!!
        val context = getBindingContext(declaration.getContainingJetFile())!!

        val shouldSpecifyType = declaration is KtNamedFunction 
            && !declaration.hasDeclaredReturnType() 
            && !KotlinBuiltIns.isUnit(declaration.returnType(context)!!)

        val editor = getActiveEditor() ?: return
        val project = KotlinEnvironment.getEnvironment(editor.javaProject!!).getProject()
        val factory = KtPsiFactory(project)

        replaceBody(declaration, factory, context, editor)

        if (shouldSpecifyType) {
        	specifyType(declaration, factory, context)
        }
    }

    private fun replaceBody(declaration: KtDeclarationWithBody, factory: KtPsiFactory, context: BindingContext, editor: KotlinFileEditor) {
        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.getViewer().getDocument())
        val indent = AlignmentStrategy.computeIndent(declaration.getNode())
        
        val newBody = convert(declaration, context, factory)
        var newBodyText = AlignmentStrategy.alignCode(newBody.getNode(), indent, lineDelimiter)

        if (declaration.getEqualsToken()!!.getNextSibling() !is PsiWhiteSpace) {
            newBodyText = factory.createWhiteSpace().getText() + newBodyText
        }

        replaceBetween(declaration.getEqualsToken()!!, declaration.getBodyExpression()!!, newBodyText)
    }
    
    private fun specifyType(declaration: KtDeclarationWithBody, factory: KtPsiFactory, context: BindingContext) {
    	val returnType = (declaration as KtNamedFunction).returnType(context).toString()
        val stringToInsert = listOf(factory.createColon(), factory.createWhiteSpace())
            .joinToString(separator = "") { it.getText()} + returnType
        insertAfter(declaration.getValueParameterList()!!, stringToInsert)
    }

    private fun convert(declaration: KtDeclarationWithBody, bindingContext: BindingContext, factory: KtPsiFactory): KtExpression {
        val body = declaration.getBodyExpression()!!

        fun generateBody(returnsValue: Boolean): KtExpression {
            val bodyType = bindingContext.getType(body)
            val needReturn = returnsValue &&
                (bodyType == null || (!KotlinBuiltIns.isUnit(bodyType) && !KotlinBuiltIns.isNothing(bodyType)))

            val expression = factory.createExpression(body.getText())
            val block: KtBlockExpression = if (needReturn) {
                    factory.createBlock("return xyz")
                }
                    else {
                    return factory.createBlock(expression.getText())
                }
            val returnExpression = PsiTreeUtil.getChildOfType(block, KtReturnExpression::class.java)
            val returned = returnExpression?.getReturnedExpression()?: return factory.createBlock("return ${expression.getText()}")
            if (KtPsiUtil.areParenthesesNecessary(expression, returned, returnExpression!!)) {
                return factory.createBlock("return (${expression.getText()})")
            }
            return factory.createBlock("return ${expression.getText()}")
        }

        val newBody = when (declaration) {
                is KtNamedFunction -> {
                val returnType = declaration.returnType(bindingContext)!!
                generateBody(!KotlinBuiltIns.isUnit(returnType) && !KotlinBuiltIns.isNothing(returnType))
            }

                is KtPropertyAccessor -> generateBody(declaration.isGetter())

                else -> throw RuntimeException("Unknown declaration type: $declaration")
            }
        return newBody
    }

    private fun KtNamedFunction.returnType(context: BindingContext): KotlinType? {
        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, this] ?: return null
        return (descriptor as FunctionDescriptor).getReturnType()
    }
}