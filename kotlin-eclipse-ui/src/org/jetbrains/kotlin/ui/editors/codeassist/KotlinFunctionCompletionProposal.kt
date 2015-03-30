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
    IN_BRACKETS
    AFTER_BRACKETS
}

public data class GenerateLambdaInfo(val lambdaType: JetType, val explicitParameters: Boolean)

public class KotlinFunctionCompletionProposal(
		proposal: ICompletionProposal,
		val caretPosition : CaretPosition,
		val lambdaInfo: GenerateLambdaInfo?): KotlinCompletionProposal(proposal), ICompletionProposalExtension2 {
	
	var caretOffset = -1
	init {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && lambdaInfo != null) {
            throw IllegalArgumentException("CaretPosition.AFTER_BRACKETS with lambdaInfo != null combination is not supported")
        }
	
    }
	
	override fun apply(viewer: ITextViewer, trigger: Char, stateMask: Int, offset: Int) {
		super<KotlinCompletionProposal>.apply(viewer.getDocument())
        addBrackets(viewer, trigger, viewer.getTextWidget().getCaretOffset())
	}
	
	override fun selected(viewer: ITextViewer, smartToggle: Boolean) {
	}
	
	override fun unselected(viewer: ITextViewer) {
	}
	
	override fun validate(document: IDocument, offset: Int, event: DocumentEvent): Boolean {
		return true
	}
	
	override fun getSelection(document: IDocument): Point = Point(caretOffset, 0)
	
	private fun addBrackets(viewer: ITextViewer, completionChar: Char, completionOffset: Int) {
		val document = viewer.getDocument()
        val chars = document.get()

        val forceParenthesis = lambdaInfo != null && completionChar == '\t' && chars.charAt(completionOffset) == '('
        val braces = lambdaInfo != null && completionChar != '(' && !forceParenthesis

        val openingBracket = if (braces) '{' else '('
        val closingBracket = if (braces) '}' else ')'

		var tailOffset = completionOffset
        var openingBracketOffset = indexOfSkippingSpace(document, openingBracket, tailOffset)
        var inBracketsShift = 0
        if (openingBracketOffset == -1) {
            if (braces) {
            	document.replace(tailOffset, 0, " {  }")
                inBracketsShift = 1
            }
            else {
            	document.replace(tailOffset, 0, "()")
            }
        }

        openingBracketOffset = indexOfSkippingSpace(document, openingBracket, tailOffset)
        assert(openingBracketOffset != -1, "If there wasn't open bracket it should already have been inserted")

        val closeBracketOffset = indexOfSkippingSpace(document, closingBracket, openingBracketOffset + 1)

        if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == -1) {
			caretOffset = openingBracketOffset + 1 + inBracketsShift
        }
        else {
        	caretOffset = closeBracketOffset + 1
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
        if (completionChar == ',' || completionChar == '.' || completionChar == '=') return false
        if (completionChar == '(') return true
        return caretPosition == CaretPosition.IN_BRACKETS
    }

}