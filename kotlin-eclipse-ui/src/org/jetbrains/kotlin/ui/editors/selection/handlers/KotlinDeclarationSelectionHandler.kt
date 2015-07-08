package org.jetbrains.kotlin.ui.editors.selection.handlers

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.JetDeclaration
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace

public class KotlinDeclarationSelectionHandler: KotlinElementSelectionHandler {
	override fun canSelect(enclosingElement: PsiElement)
		= enclosingElement is JetDeclaration
	
	override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		val bodyRange = getBodyRange(enclosingElement)
		if (bodyRange.contains(selectionCandidate.getTextRange())) {
			return selectEnclosing(enclosingElement, selectedRange)
		}
		if (selectionCandidate is PsiComment) {
			return TextRange(selectedRange.getStartOffset(), selectionCandidate.getTextRange().getEndOffset())
		}
		return enclosingElement.getTextRange()
	}
	
	override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		val bodyRange = getBodyRange(enclosingElement)
		if (bodyRange.contains(selectionCandidate.getTextRange())) {
			return selectEnclosing(enclosingElement, selectedRange)
		}
		if (selectionCandidate is PsiComment) {
			return TextRange(selectionCandidate.getTextRange().getStartOffset(), selectedRange.getEndOffset())
		}
		return enclosingElement.getTextRange()
	}
	
	override fun selectEnclosing(enclosingElement: PsiElement, selectedRange: TextRange): TextRange {
		val bodyRange = getBodyRange(enclosingElement)
		if (!bodyRange.contains(selectedRange) || bodyRange.equals(selectedRange)) {
			return enclosingElement.getTextRange()
		}
		return bodyRange;
	}
	
	private fun getBodyRange(enclosingElement: PsiElement): TextRange {
		var firstNonCommentChild = enclosingElement.getFirstChild()
		while (firstNonCommentChild!=null && (firstNonCommentChild is PsiWhiteSpace || firstNonCommentChild is PsiComment)) {
			firstNonCommentChild = firstNonCommentChild.getNextSibling()
		}
		var lastNonCommentChild = enclosingElement.getLastChild()
		while (lastNonCommentChild!=null && (lastNonCommentChild is PsiWhiteSpace || lastNonCommentChild is PsiComment)) {
			lastNonCommentChild = lastNonCommentChild.getPrevSibling()
		}
		if (firstNonCommentChild == null || lastNonCommentChild == null) {
			return enclosingElement.getTextRange() //WHY SO SERIOUS???
		}
		return TextRange(firstNonCommentChild.getTextRange().getStartOffset(), lastNonCommentChild.getTextRange().getEndOffset())
	}
}