package org.jetbrains.kotlin.ui.editors.selection.handlers

import org.jetbrains.kotlin.psi.JetWhenExpression
import org.jetbrains.kotlin.psi.JetBlockExpression
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.JetTokens
import com.intellij.psi.PsiWhiteSpace
import com.intellij.openapi.util.TextRange

public class KotlinBlockSelectionHandler: KotlinDefaultSelectionHandler() {
	override fun canSelect(enclosingElement: PsiElement)
		= enclosingElement is JetBlockExpression || enclosingElement is JetWhenExpression

	override fun selectEnclosing(enclosingElement: PsiElement, selectedRange: TextRange): TextRange {
		val elementStart = findBlockContentStart(enclosingElement);
		val elementEnd = findBlockContentEnd(enclosingElement)
		if (elementStart >= elementEnd) {
			return enclosingElement.getTextRange()
		}
		val resultRange = TextRange(elementStart, elementEnd)
		if (resultRange.equals(selectedRange) || !resultRange.contains(selectedRange)) {
			return enclosingElement.getTextRange()
		}
		return resultRange;
	}

	override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		if (selectionCandidate.getNode().getElementType() == JetTokens.LBRACE) {
			return selectEnclosing(enclosingElement, selectedRange)
		}
		return super.selectPrevious(enclosingElement, selectionCandidate, selectedRange)
	}


	override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		if (selectionCandidate.getNode().getElementType() == JetTokens.RBRACE) {
			return selectEnclosing(enclosingElement, selectedRange)
		}
		return super.selectNext(enclosingElement, selectionCandidate, selectedRange)
	}

	private fun findBlockContentStart(block: PsiElement): Int {
		var element = block.getFirstChild()
		while (element!= null && element.getNode().getElementType() != JetTokens.LBRACE) {
			element = element.getNextSibling()
		}
		if (element == null || element.getNextSibling() == null) {
			return block.getTextRange().getStartOffset()
		}
		//skip brace
		element = element.getNextSibling()
		while (element!= null && element is PsiWhiteSpace) {
			element = element.getNextSibling()
		}
		if (element == null) {
			return block.getTextRange().getStartOffset()
		}
		return element.getTextRange().getStartOffset()
	}

	private fun findBlockContentEnd(block: PsiElement): Int {
		var element = block.getLastChild()
		while (element!= null && element.getNode().getElementType() != JetTokens.RBRACE) {
			element = element.getPrevSibling()
		}
		if (element == null || element.getPrevSibling() == null) {
			return block.getTextRange().getEndOffset()
		}
		//skip brace
		element = element.getPrevSibling()
		while (element!= null && element is PsiWhiteSpace) {
			element = element.getPrevSibling()
		}
		if (element == null) {
			return block.getTextRange().getEndOffset()
		}
		return element.getTextRange().getEndOffset()
	}
}