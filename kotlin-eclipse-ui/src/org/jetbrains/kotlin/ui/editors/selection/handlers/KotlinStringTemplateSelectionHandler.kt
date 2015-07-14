package org.jetbrains.kotlin.ui.editors.selection.handlers

import org.jetbrains.kotlin.psi.JetStringTemplateExpression
import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange

public class KotlinStringTemplateSelectionHandler: KotlinDefaultSelectionHandler() {
	override fun canSelect(enclosingElement: PsiElement)
		= enclosingElement is JetStringTemplateExpression
	
	override fun selectEnclosing(enclosingElement: PsiElement, selectedRange: TextRange): TextRange {
		val literalRange = super.selectEnclosing(enclosingElement, selectedRange)
		val resultRange = TextRange(literalRange.getStartOffset()+1, literalRange.getEndOffset()-1)
		if (!resultRange.contains(selectedRange) || resultRange.equals(selectedRange)) {
			return literalRange
		}
		//drop quotes
		return TextRange(literalRange.getStartOffset()+1, literalRange.getEndOffset()-1)
	}
	
	override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange): TextRange {
		if (selectionCandidate.getNextSibling() == null) {// is quote sign
			return enclosingElement.getTextRange()
		}
		return selectionWithElementAppendedToEnd(selectedRange, selectionCandidate)
	}
	
	override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate:PsiElement, selectedRange: TextRange): TextRange {
		if (selectionCandidate.getPrevSibling() == null) {// is quote sign
			return enclosingElement.getTextRange()
		}
		return selectionWithElementAppendedToBeginning(selectedRange, selectionCandidate)
	}
}