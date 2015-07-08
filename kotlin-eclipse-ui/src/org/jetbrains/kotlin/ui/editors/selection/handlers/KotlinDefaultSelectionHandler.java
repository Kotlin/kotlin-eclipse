package org.jetbrains.kotlin.ui.editors.selection.handlers;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

public class KotlinDefaultSelectionHandler implements KotlinElementSelectionHandler {
    
    @Override
    public boolean canSelect(PsiElement enclosingElement) {
        return true;
    }
    
    @Override
    public TextRange selectEnclosing(PsiElement enclosingElement, TextRange selectedRange) {
        return enclosingElement.getTextRange();
    }
    
    @Override
    public TextRange selectPrevious(PsiElement enclosingElement, PsiElement selectionCandidate, TextRange selectedRange) {
        return new TextRange(selectionCandidate.getTextRange().getStartOffset(), selectedRange.getEndOffset());
    }
    
    @Override
    public TextRange selectNext(PsiElement enclosingElement, PsiElement selectionCandidate, TextRange selectedRange) {
        return new TextRange(selectedRange.getStartOffset(), selectionCandidate.getTextRange().getEndOffset());
    }
    
}
