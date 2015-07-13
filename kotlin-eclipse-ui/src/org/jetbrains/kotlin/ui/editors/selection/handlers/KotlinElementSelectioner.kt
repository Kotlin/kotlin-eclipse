package org.jetbrains.kotlin.ui.editors.selection.handlers

import java.util.ArrayList
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

public object KotlinElementSelectioner {
    private val selectionHandlers = listOf(
        KotlinListSelectionHandler(),
        KotlinBlockSelectionHandler(),
        KotlinWhiteSpaceSelectionHandler(),
        KotlinDocSectionSelectionHandler(),
        KotlinDeclarationSelectionHandler(),
        KotlinStringTemplateSelectionHandler(),
        KotlinNonTraversableSelectionHanlder()//must be last
        )

    private val defaultHandler = KotlinDefaultSelectionHandler()

    public fun selectEnclosing(enclosingElement:PsiElement, selectedRange:TextRange):TextRange
    	= findHandler(enclosingElement).selectEnclosing(enclosingElement, selectedRange)

    public fun selectNext(enclosingElement:PsiElement, selectionCandidate:PsiElement, selectedRange:TextRange):TextRange
    	= findHandler(enclosingElement).selectNext(enclosingElement, selectionCandidate, selectedRange)

    public fun selectPrevious(enclosingElement:PsiElement, selectionCandidate:PsiElement, selectedRange:TextRange):TextRange
    	= findHandler(enclosingElement).selectPrevious(enclosingElement, selectionCandidate, selectedRange)

    private fun findHandler(enclosingElement:PsiElement)
	    = selectionHandlers.firstOrNull {
	        it.canSelect(enclosingElement)
	    }
	    ?: defaultHandler
}