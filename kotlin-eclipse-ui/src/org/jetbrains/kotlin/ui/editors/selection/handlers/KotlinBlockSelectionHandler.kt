package org.jetbrains.kotlin.ui.editors.selection.handlers

import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import com.intellij.psi.PsiWhiteSpace
import com.intellij.openapi.util.TextRange

public class KotlinBlockSelectionHandler: KotlinDefaultSelectionHandler() {
	override fun canSelect(enclosingElement: PsiElement)
		= enclosingElement is KtBlockExpression || enclosingElement is KtWhenExpression

	override fun selectEnclosing(enclosingElement: PsiElement, selectedRange: TextRange): TextRange {
		val elementStart = findBlockContentStart(enclosingElement);
		val elementEnd = findBlockContentEnd(enclosingElement)
		if (elementStart >= elementEnd) {
			return enclosingElement.getTextRange()
		}
		val resultRange = TextRange(elementStart, elementEnd)
		if (resultRange == selectedRange || selectedRange !in resultRange) {
			return enclosingElement.getTextRange()
		}
		return resultRange;
	}

	override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		if (selectionCandidate.getNode().getElementType() == KtTokens.LBRACE) {
			return selectEnclosing(enclosingElement, selectedRange)
		}
		return selectionWithElementAppendedToBeginning(selectedRange, selectionCandidate)
	}

	override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		if (selectionCandidate.getNode().getElementType() == KtTokens.RBRACE) {
			return selectEnclosing(enclosingElement, selectedRange)
		}
		return selectionWithElementAppendedToEnd(selectedRange, selectionCandidate)
	}

	private fun findBlockContentStart(block: PsiElement): Int {
	    val element = block.allChildren
        .dropWhile { it.getNode().getElementType() != KtTokens.LBRACE }
        .drop(1)
        .dropWhile { it is PsiWhiteSpace }
        .firstOrNull() ?: block
		return element.getTextRange().getStartOffset()
	}

	private fun findBlockContentEnd(block: PsiElement): Int {
	    val element = block.allChildren
            .toList()
            .reversed()
            .asSequence()
            .dropWhile { it.getNode().getElementType() != KtTokens.RBRACE }
            .drop(1)
            .dropWhile { it is PsiWhiteSpace }
            .firstOrNull() ?: block
		return element.getTextRange().getEndOffset()
	}
}