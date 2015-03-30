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

public enum class CaretPosition {
    IN_BRACKETS
    AFTER_BRACKETS
}

public data class GenerateLambdaInfo(val lambdaType: JetType, val explicitParameters: Boolean)

public class KotlinFunctionCompletionProposal(
		proposal: ICompletionProposal,
		val jetFile: JetFile,
		val editor: KotlinEditor,
		val caretPosition : CaretPosition,
		val lambdaInfo: GenerateLambdaInfo?): KotlinCompletionProposal(proposal), ICompletionProposalExtension {
	
	init {
        if (caretPosition == CaretPosition.AFTER_BRACKETS && lambdaInfo != null) {
            throw IllegalArgumentException("CaretPosition.AFTER_BRACKETS with lambdaInfo != null combination is not supported")
        }
    }
	
	override fun apply(document: IDocument, trigger: Char, offset: Int) {
		super<KotlinCompletionProposal>.apply(document)
		
        val startOffset = LineEndUtil.convertCrToDocumentOffset(document, offset)
        val element = jetFile.findElementAt(startOffset) ?: return

        when {
            PsiTreeUtil.getParentOfType(element, javaClass<JetImportDirective>(), true) != null -> return

            else -> addBrackets(document, trigger, editor.getViewer().getTextWidget().getCaretOffset())
        }
	}
	
	override fun isValidFor(document: IDocument, offset: Int): Boolean {
		return true
	}
	
	override fun getTriggerCharacters(): CharArray? {
		return null
	}
	
	override fun getContextInformationPosition(): Int {
		return -1
	}
	
	private fun addBrackets(document: IDocument, completionChar: Char, completionOffset: Int) {
        val chars = document.get()

        val forceParenthesis = lambdaInfo != null && completionChar == '\t' && chars.charAt(completionOffset) == '('
        val braces = lambdaInfo != null && completionChar != '(' && !forceParenthesis

        val openingBracket = if (braces) '{' else '('
        val closingBracket = if (braces) '}' else ')'

		var offset = completionOffset
        if (completionChar == '\t') {
            offset = skipSpaces(chars, offset)
            if (offset < document.get().length) {
                if (chars[offset] == '<') {
//                    PsiDocumentManager.getInstance(context.getProject()).commitDocument(document)
                    val token = jetFile.findElementAt(offset)
                    if (token.getNode().getElementType() == JetTokens.LT) {
                        val parent = token.getParent()
                        if (parent is JetTypeArgumentList && parent.getText().indexOf('\n') < 0/* if type argument list is on multiple lines this is more likely wrong parsing*/) {
                            offset = parent.getTextRange().getEndOffset()
                        }
                    }
                }
            }
        }

        var openingBracketOffset = indexOfSkippingSpace(document, openingBracket, offset)
        var inBracketsShift = 0
		val shiftedOffset = LineEndUtil.convertLfToDocumentOffset(jetFile.getText(), offset, document)
        if (openingBracketOffset == -1) {
            if (braces) {
            	document.replace(shiftedOffset, 0, " {  }")
                inBracketsShift = 1
            }
            else {
            	document.replace(shiftedOffset, 0, "()")
            }
        }

        openingBracketOffset = indexOfSkippingSpace(document, openingBracket, offset)
        assert(openingBracketOffset != -1, "If there wasn't open bracket it should already have been inserted")

        val closeBracketOffset = indexOfSkippingSpace(document, closingBracket, openingBracketOffset + 1)

        if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == -1) {
        	val withourLf = LineEndUtil.convertLfToDocumentOffset(jetFile.getText(), openingBracketOffset + 1 + inBracketsShift, document)
        	editor.setHighlightRange(withourLf, 0, true)
        }
        else {
        	editor.setHighlightRange(closeBracketOffset + 1, 0, true)
        }
    }
	
	private fun skipSpaces(chars: CharSequence, index : Int) : Int
                = (index..chars.length() - 1).firstOrNull { val c = chars[it]; c != ' ' && c != '\t' } ?: chars.length()
	
	        private fun indexOfSkippingSpace(document: IDocument, ch : Char, startIndex : Int) : Int {
            val text = document.get()
            for (i in startIndex..text.length() - 1) {
                val currentChar = text[i]
                if (ch == currentChar) return i
                if (currentChar != ' ' && currentChar != '\t') return -1
            }
            return -1
        }
	
	private fun isInsertSpacesInOneLineFunctionEnabled() = true
	
	private fun shouldPlaceCaretInBrackets(completionChar: Char): Boolean {
        if (completionChar == ',' || completionChar == '.' || completionChar == '=') return false
        if (completionChar == '(') return true
        return caretPosition == CaretPosition.IN_BRACKETS
    }


}