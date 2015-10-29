package org.jetbrains.kotlin.ui.editors.selection.handlers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection

public class KotlinListSelectionHandler: KotlinDefaultSelectionHandler() {
    override fun canSelect(enclosingElement: PsiElement) =
	    enclosingElement is KtParameterList ||
	    enclosingElement is KtValueArgumentList ||
	    enclosingElement is KtTypeParameterList ||
	    enclosingElement is KtTypeArgumentList

    override fun selectEnclosing(enclosingElement: PsiElement, selectedRange: TextRange): TextRange {
        val elementRange = enclosingElement.getTextRange();
        //delete parentheses
        val resultRange = TextRange(elementRange.getStartOffset() + 1, elementRange.getEndOffset()-1)
        if (resultRange == selectedRange || selectedRange !in resultRange) {
            return KotlinElementSelectioner.selectEnclosing(enclosingElement.getParent(), selectedRange)
        }
        return resultRange
    }

    private fun isElementOfListPartType(element: PsiElement): Boolean =
	    element is KtValueArgument || element is KtParameter ||
	    element is KtTypeParameter || element is KtTypeProjection

    override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
        var elementToSelect = selectionCandidate.siblings(forward = false, withItself = true)
            .firstOrNull { isElementOfListPartType(it) }
        if (elementToSelect == null) {
            return selectEnclosing(enclosingElement, selectedRange)
        }
        return selectionWithElementAppendedToBeginning(selectedRange, elementToSelect)
    }

    override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
        var elementToSelect = selectionCandidate.siblings(forward = true, withItself = true)
            .firstOrNull { isElementOfListPartType(it) }
        if (elementToSelect == null) {
            return selectEnclosing(enclosingElement, selectedRange)
        }
        return selectionWithElementAppendedToEnd(selectedRange, elementToSelect)
    }
}