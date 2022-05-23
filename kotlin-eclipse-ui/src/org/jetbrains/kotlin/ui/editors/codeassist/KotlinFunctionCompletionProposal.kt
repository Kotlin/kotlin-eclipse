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
package org.jetbrains.kotlin.ui.editors.codeassist

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6
import org.eclipse.swt.graphics.Point

enum class CaretPosition {
    IN_BRACKETS,
    AFTER_BRACKETS
}

class KotlinFunctionCompletionProposal(
    private val proposal: KotlinCompletionProposal,
    private val caretPosition: CaretPosition,
    private val hasLambda: Boolean,
    private val lambdaParamNames: String = ""
) : ICompletionProposal by proposal, ICompletionProposalExtension2 by proposal,
    ICompletionProposalExtension6 by proposal, KotlinTypedCompletionProposal by proposal,
    KotlinRelevanceCompletionProposal by proposal {

    init {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && hasLambda) {
            throw IllegalArgumentException("CaretPosition.AFTER_BRACKETS with lambdaInfo != null combination is not supported")
        }
    }

    override fun apply(viewer: ITextViewer, trigger: Char, stateMask: Int, offset: Int) {
        proposal.apply(viewer, trigger, stateMask, offset)

        addBrackets(viewer, trigger, proposal.getSelection(viewer.document)!!.x)
        if (trigger == '.') {
            val closeBracketOffset = viewer.textWidget.caretOffset
            viewer.document.replace(closeBracketOffset, 0, trigger.toString())
            viewer.textWidget.caretOffset = closeBracketOffset + 1
        }
    }

    override fun getSelection(document: IDocument): Point? = null


    private fun addBrackets(viewer: ITextViewer, completionChar: Char, completionOffset: Int) {
        val document = viewer.document
        val braces = hasLambda && completionChar != '('

        val openingBracket = if (braces) '{' else '('
        val closingBracket = if (braces) '}' else ')'

        var openingBracketOffset = indexOfSkippingSpace(document, openingBracket, completionOffset)
        var inBracketsShift = 0
        if (openingBracketOffset == -1) {
            if (braces) {
                document.replace(completionOffset, 0, " { $lambdaParamNames }")
                inBracketsShift = 1
            } else {
                document.replace(completionOffset, 0, "()")
            }
        }

        openingBracketOffset = indexOfSkippingSpace(document, openingBracket, completionOffset)
        assert(openingBracketOffset != -1) { "If there wasn't open bracket it should already have been inserted" }

        val closeBracketOffset = indexOfSkippingSpace(document, closingBracket, openingBracketOffset + 1)

        if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == -1) {
            viewer.setSelectedRange(openingBracketOffset + 1 + inBracketsShift, 0)
        } else {
            viewer.setSelectedRange(closeBracketOffset + 1, 0)
        }
    }

    private fun indexOfSkippingSpace(document: IDocument, ch: Char, startIndex: Int): Int {
        val text = document.get()
        for (i in startIndex until text.length) {
            val currentChar = text[i]
            if (ch == currentChar) return i
            if (currentChar != ' ' && currentChar != '\t') return -1
        }
        return -1
    }

    private fun shouldPlaceCaretInBrackets(completionChar: Char): Boolean {
        return when (completionChar) {
            '.' -> false
            '(' -> true
            else -> caretPosition == CaretPosition.IN_BRACKETS
        }
    }
}
