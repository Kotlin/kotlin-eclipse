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

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.jface.text.TextSelection
import org.eclipse.jface.text.Position
import org.eclipse.swt.graphics.Point

public enum class CaretPosition {
    IN_BRACKETS,
    AFTER_BRACKETS
}

public class KotlinFunctionCompletionProposal(
        proposal: KotlinCompletionProposal,
        val caretPosition: CaretPosition,
        val hasLambda: Boolean,
        identifierPart: String) :
            KotlinCompletionProposal(
                    proposal.replacementString,
                    proposal.img,
                    proposal.presentableString,
                    identifierPart = identifierPart) {
    
    init {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && hasLambda) {
            throw IllegalArgumentException("CaretPosition.AFTER_BRACKETS with lambdaInfo != null combination is not supported")
        }
    }
    
    override fun apply(viewer: ITextViewer, trigger: Char, stateMask: Int, offset: Int) {
        super.apply(viewer, trigger, stateMask, offset)
        
        addBrackets(viewer, trigger, super.getSelection(viewer.getDocument())!!.x)
        if (trigger == '.') {
            val closeBracketOffset = viewer.getTextWidget().getCaretOffset()
            viewer.getDocument().replace(closeBracketOffset, 0, trigger.toString())
            viewer.getTextWidget().setCaretOffset(closeBracketOffset + 1)
        }
    }
    
    override fun getSelection(document: IDocument): Point? = null
    
    
    private fun addBrackets(viewer: ITextViewer, completionChar: Char, completionOffset: Int) {
        val document = viewer.getDocument()
        val braces = hasLambda && completionChar != '('
        
        val openingBracket = if (braces) '{' else '('
        val closingBracket = if (braces) '}' else ')'
        
        var openingBracketOffset = indexOfSkippingSpace(document, openingBracket, completionOffset)
        var inBracketsShift = 0
        if (openingBracketOffset == -1) {
            if (braces) {
                document.replace(completionOffset, 0, " {  }")
                inBracketsShift = 1
            }
                else {
                document.replace(completionOffset, 0, "()")
            }
        }
        
        openingBracketOffset = indexOfSkippingSpace(document, openingBracket, completionOffset)
        assert(openingBracketOffset != -1) { "If there wasn't open bracket it should already have been inserted" }
        
        val closeBracketOffset = indexOfSkippingSpace(document, closingBracket, openingBracketOffset + 1)
        
        if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == -1) {
            viewer.setSelectedRange(openingBracketOffset + 1 + inBracketsShift, 0)
        }
            else {
            viewer.setSelectedRange(closeBracketOffset + 1, 0)
        }
    }
    
    private fun indexOfSkippingSpace(document: IDocument, ch : Char, startIndex : Int) : Int {
        val text = document.get()
        for (i in startIndex..text.length - 1) {
            val currentChar = text[i]
            if (ch == currentChar) return i
            if (currentChar != ' ' && currentChar != '\t') return -1
        }
        return -1
    }
    
    private fun shouldPlaceCaretInBrackets(completionChar: Char): Boolean {
        return when {
            completionChar == '.' -> false
            completionChar == '(' -> true
            else -> caretPosition == CaretPosition.IN_BRACKETS
        }
    }
}