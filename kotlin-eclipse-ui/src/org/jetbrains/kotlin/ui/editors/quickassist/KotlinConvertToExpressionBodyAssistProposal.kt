/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.TextUtilities
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.eclipse.ui.utils.getBindingContext
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.selection.handlers.siblings
import org.jetbrains.kotlin.ui.formatter.formatCode
import org.jetbrains.kotlin.types.isError

public class KotlinConvertToExpressionBodyAssistProposal(editor: KotlinEditor) : KotlinQuickAssistProposal(editor) {
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
        val analysisResultWithProvider = getAnalysisResultWithProvider(declaration.getContainingKtFile())
        val context = analysisResultWithProvider.analysisResult.bindingContext
        val value = calcValue(declaration, context)!!

        val setUnitType: Boolean = if (!declaration.hasDeclaredReturnType() && declaration is KtNamedFunction) {
            val valueType = context.getType(value)
            valueType != null && !KotlinBuiltIns.isUnit(valueType)
        } else {
            false
        }

        replaceBody(declaration, value, editor)

        val omitType = (declaration.hasDeclaredReturnType() || setUnitType) &&
                declaration is KtCallableDeclaration

        insertAndSelectType(declaration, setUnitType, omitType, editor)

    }

    private fun replaceBody(declaration: KtDeclarationWithBody, newBody: KtExpression, editor: KotlinEditor) {
        val body = declaration.getBodyExpression()!!
        val psiFactory = KtPsiFactory(declaration)
        val eqToken = psiFactory.createEQ().getText()

        val lineDelimiter = TextUtilities.getDefaultLineDelimiter(editor.javaEditor.getViewer().getDocument())
        val file = editor.eclipseFile ?: return
        val valueText = formatCode(newBody.node.text, file.name, psiFactory, lineDelimiter)

        replace(body, "$eqToken $valueText")
    }

    private fun insertAndSelectType(declaration: KtDeclarationWithBody, setUnitType: Boolean, omitType: Boolean, editor: KotlinEditor) {
        val body = declaration.getBodyExpression()!!

        if (omitType && !setUnitType) {
            val callableDeclaration = declaration as KtCallableDeclaration
            val typeRef = callableDeclaration.getTypeReference()!!
            val colon = callableDeclaration.getColon()!!
            val range = TextRange(getStartOffset(colon, editor), getEndOffset(typeRef, editor))
            editor.javaEditor.selectAndReveal(range.getStartOffset(), range.getLength())
        }
        if (setUnitType) {
            val elementToPlaceTypeAfter = body.siblings(forward = false, withItself = false).
                    first { it !is PsiWhiteSpace }
            val offset = getEndOffset(elementToPlaceTypeAfter, editor)
            val stringToInsert = ": ${DefaultBuiltIns.Instance.getUnitType().toString()}"
            insertAfter(elementToPlaceTypeAfter, stringToInsert)
            if (omitType) {
                editor.javaEditor.selectAndReveal(offset, stringToInsert.length)
            }
        }
    }

    private fun calcValue(declaration: KtDeclarationWithBody, context: BindingContext): KtExpression? {
        if (declaration is KtFunctionLiteral) return null
        val body = declaration.getBodyExpression()
        if (!declaration.hasBlockBody() || body !is KtBlockExpression) return null

        val statement = body.getStatements().singleOrNull() ?: return null
        when (statement) {
            is KtReturnExpression -> {
                return statement.getReturnedExpression()
            }

            //TODO: IMO this is not good code, there should be a way to detect that KtExpression does not have value
            is KtDeclaration, is KtLoopExpression -> return null // is KtExpression but does not have value

            else -> {
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
    if (type.isError) return
    val typeReference = KtPsiFactory(getProject()).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
    setTypeReference(typeReference)
}