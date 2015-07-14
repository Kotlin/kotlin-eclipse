package org.jetbrains.kotlin.ui.editors.selection.handlers

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.JetDeclaration
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace

public class KotlinDeclarationSelectionHandler: KotlinDefaultSelectionHandler() {
    override fun canSelect(enclosingElement: PsiElement)
    = enclosingElement is JetDeclaration

    override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
        val bodyRange = getBodyRange(enclosingElement)
        if (selectionCandidate.getTextRange() in bodyRange) {
            return selectEnclosing(enclosingElement, selectedRange)
        }
        if (selectionCandidate is PsiComment) {
            return selectionWithElementAppendedToEnd(selectedRange, selectionCandidate)
        }
        return enclosingElement.getTextRange()
    }

    override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
        val bodyRange = getBodyRange(enclosingElement)
        if (selectionCandidate.getTextRange() in bodyRange) {
            return selectEnclosing(enclosingElement, selectedRange)
        }
        if (selectionCandidate is PsiComment) {
            return selectionWithElementAppendedToBeginning(selectedRange, selectionCandidate)
        }
        return enclosingElement.getTextRange()
    }

    override fun selectEnclosing(enclosingElement: PsiElement, selectedRange: TextRange): TextRange {
        val bodyRange = getBodyRange(enclosingElement)
        if (selectedRange !in bodyRange || bodyRange == selectedRange) {
            return enclosingElement.getTextRange()
        }
        return bodyRange;
    }

    private fun getBodyRange(enclosingElement: PsiElement): TextRange {

        val firstNonCommentChild = enclosingElement.allChildren
            .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
            ?: enclosingElement.getFirstChild()

        var lastNonCommentChild = enclosingElement.allChildren
            .toList()
            .reverse()
            .asSequence()
            .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
            ?: enclosingElement.getLastChild()

        return TextRange(firstNonCommentChild.startOffset, lastNonCommentChild.endOffset)
    }
}