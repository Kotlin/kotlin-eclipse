package org.jetbrains.kotlin.ui.editors.selection.handlers

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtExpression

public class KotlinNonTraversableSelectionHanlder: KotlinDefaultSelectionHandler() {
	override fun canSelect(enclosingElement: PsiElement) = 
			enclosingElement is KDoc ||
			enclosingElement is KtExpression //must be the last before default 
	
	override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange) = 
		selectEnclosing(enclosingElement, selectedRange)
	
	override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange) = 
		selectEnclosing(enclosingElement, selectedRange)
}