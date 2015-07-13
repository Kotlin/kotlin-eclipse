package org.jetbrains.kotlin.ui.editors.selection.handlers

import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange

public class KotlinDocSectionSelectionHandler: KotlinElementSelectionHandler {
	override fun canSelect(enclosingElement: PsiElement)
		= enclosingElement is KDocSection

	override fun selectEnclosing(enclosingElement: PsiElement, selectedRange: TextRange)
		= KotlinElementSelectioner.selectEnclosing(enclosingElement.getParent(), selectedRange)

	override fun selectNext(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange)
		= selectEnclosing(enclosingElement, selectedRange)

	override fun selectPrevious(enclosingElement: PsiElement, selectionCandidate: PsiElement, selectedRange: TextRange)
		= selectEnclosing(enclosingElement, selectedRange)
}