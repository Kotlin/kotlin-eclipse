package org.jetbrains.kotlin.ui.editors.codeassist

import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.JetImportDirective
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetTypeArgumentList
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.ui.editors.KotlinEditor
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
		val caretPosition : CaretPosition,
		val hasLambda: Boolean): KotlinHandlerCompletionProposal(proposal) {
	
	init {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && hasLambda) {
            throw IllegalArgumentException("CaretPosition.AFTER_BRACKETS with lambdaInfo != null combination is not supported")
        }
    }
	
	override fun apply(viewer: ITextViewer, trigger: Char, stateMask: Int, offset: Int) {
		val replacementLength = offset - proposal.replacementOffset
		viewer.getDocument().replace(proposal.replacementOffset, replacementLength, proposal.replacementString)
		
        addBrackets(viewer, trigger, super.getSelection(viewer.getDocument())!!.x)
		if (trigger == '.') {
			val closeBracketOffset = viewer.getTextWidget().getCaretOffset()
			viewer.getDocument().replace(closeBracketOffset, 0, trigger.toString())
			viewer.getTextWidget().setCaretOffset(closeBracketOffset + 1)
		}
	}
	
	override fun getSelection(document: IDocument): Point? = null
	
	override fun getTriggerCharacters(): CharArray {
		return charArrayOf('(', '.')
	}
	
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
        assert(openingBracketOffset != -1, "If there wasn't open bracket it should already have been inserted")

        val closeBracketOffset = indexOfSkippingSpace(document, closingBracket, openingBracketOffset + 1)

        if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == -1) {
			viewer.getTextWidget().setCaretOffset(openingBracketOffset + 1 + inBracketsShift)
        }
        else {
			viewer.getTextWidget().setCaretOffset(closeBracketOffset + 1)
        }
    }
	
    private fun indexOfSkippingSpace(document: IDocument, ch : Char, startIndex : Int) : Int {
        val text = document.get()
        for (i in startIndex..text.length() - 1) {
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