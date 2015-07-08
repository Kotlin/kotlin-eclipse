package org.jetbrains.kotlin.ui.editors.selection.handlers;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

public interface KotlinElementSelectionHandler {
    public boolean canSelect(PsiElement enclosingElement);
    
    public TextRange selectEnclosing(PsiElement enclosingElement, TextRange selectedRange);
    
    public TextRange selectPrevious(PsiElement enclosingElement, PsiElement selectionCandidate, TextRange selectedRange);
    
    public TextRange selectNext(PsiElement enclosingElement, PsiElement selectionCandidate, TextRange selectedRange);    
}
