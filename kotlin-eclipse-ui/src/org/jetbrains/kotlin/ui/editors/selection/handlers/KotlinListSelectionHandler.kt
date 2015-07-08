package org.jetbrains.kotlin.ui.editors.selection.handlers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetParameterList
import org.jetbrains.kotlin.psi.JetValueArgumentList
import org.jetbrains.kotlin.psi.JetTypeParameterList
import org.jetbrains.kotlin.psi.JetTypeArgumentList
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.JetValueArgument
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetTypeParameter
import org.jetbrains.kotlin.psi.JetTypeProjection

public class KotlinListSelectionHandler: KotlinElementSelectionHandler {
	override fun canSelect(enclosingElement: PsiElement) =
	enclosingElement is JetParameterList ||
	enclosingElement is JetValueArgumentList ||
	enclosingElement is JetTypeParameterList ||
	enclosingElement is JetTypeArgumentList

	override fun selectEnclosing(enclosingElement: PsiElement, selectedRange: TextRange): TextRange {
		val elementRange = enclosingElement.getTextRange();
		//delete parentheses
		val resultRange = TextRange(elementRange.getStartOffset()+1, elementRange.getEndOffset()-1)
		if (resultRange.equals(selectedRange) || !resultRange.contains(selectedRange)) {
			return KotlinElementSelectioner.getInstance().selectEnclosing(enclosingElement.getParent(), selectedRange)
		}
		return resultRange
	}

	private fun isElementOfListPartType(element: PsiElement): Boolean =
	(element is JetValueArgument || element is JetParameter ||
	element is JetTypeParameter ||	element is JetTypeProjection)

	override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		var currentSelectionCandidate: PsiElement? = selectionCandidate
		while (currentSelectionCandidate != null && !isElementOfListPartType(currentSelectionCandidate)) {
			currentSelectionCandidate = currentSelectionCandidate.getPrevSibling()
		}
		if (currentSelectionCandidate == null) {
			return selectEnclosing(enclosingElement, selectedRange)
		}
		val elementRange = currentSelectionCandidate.getTextRange();
		return TextRange(elementRange.getStartOffset(), selectedRange.getEndOffset())
	}

	override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		var currentSelectionCandidate: PsiElement? = selectionCandidate
		while (currentSelectionCandidate != null && !isElementOfListPartType(currentSelectionCandidate)) {
			currentSelectionCandidate = currentSelectionCandidate.getNextSibling()
		}
		if (currentSelectionCandidate == null) {
			return selectEnclosing(enclosingElement, selectedRange)
		}
		val elementRange = currentSelectionCandidate.getTextRange();
		return TextRange(selectedRange.getStartOffset(), elementRange.getEndOffset())
	}
}