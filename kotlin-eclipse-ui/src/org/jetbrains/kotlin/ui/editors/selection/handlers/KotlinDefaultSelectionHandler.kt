package org.jetbrains.kotlin.ui.editors.selection.handlers

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

public open class KotlinDefaultSelectionHandler:KotlinElementSelectionHandler {
    
    override fun canSelect(enclosingElement:PsiElement)
        = true

    override fun selectEnclosing(enclosingElement:PsiElement, selectedRange:TextRange)
        = enclosingElement.getTextRange()

    override fun selectPrevious(enclosingElement:PsiElement, selectionCandidate:PsiElement, selectedRange:TextRange)
        = selectionWithElementAppendedToBeginning(selectedRange, selectionCandidate)

    override fun selectNext(enclosingElement:PsiElement, selectionCandidate:PsiElement, selectedRange:TextRange)
        = selectionWithElementAppendedToEnd(selectedRange, selectionCandidate)

     protected fun selectionWithElementAppendedToEnd(selection: TextRange, element: PsiElement):TextRange
     	= TextRange(selection.getStartOffset(), element.endOffset)
    
    protected fun selectionWithElementAppendedToBeginning(selection: TextRange, element: PsiElement):TextRange
     	= TextRange(element.startOffset, selection.getEndOffset())
}