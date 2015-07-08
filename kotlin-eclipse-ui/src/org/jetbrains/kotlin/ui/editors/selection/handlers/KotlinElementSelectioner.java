package org.jetbrains.kotlin.ui.editors.selection.handlers;

import java.util.ArrayList;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

public class KotlinElementSelectioner {    
    
    private final ArrayList<KotlinElementSelectionHandler> selectionHandlers;
    private final KotlinDefaultSelectionHandler defaultHandler;
    
    private KotlinElementSelectioner() {
        selectionHandlers = new ArrayList<>();
        selectionHandlers.add(new KotlinListSelectionHandler());
        selectionHandlers.add(new KotlinBlockSelectionHandler());
        selectionHandlers.add(new KotlinWhiteSpaceSelectionHandler());
        selectionHandlers.add(new KotlinDocSectionSelectionHandler());
        selectionHandlers.add(new KotlinDeclarationSelectionHandler());
        selectionHandlers.add(new KotlinStringTemplateSelectionHandler());
        selectionHandlers.add(new KotlinNonTraversableSelectionHanlder());//must be last
        defaultHandler = new KotlinDefaultSelectionHandler();
    }
    
    public TextRange selectEnclosing(PsiElement enclosingElement, TextRange selectedRange) {
        KotlinElementSelectionHandler handler = findHandler(enclosingElement);
        return handler.selectEnclosing(enclosingElement, selectedRange);
    }
    
    public TextRange selectNext(PsiElement enclosingElement, PsiElement selectionCandidate, TextRange selectedRange) {
        KotlinElementSelectionHandler handler = findHandler(enclosingElement);
        return handler.selectNext(enclosingElement, selectionCandidate, selectedRange);
    }
    
    public TextRange selectPrevious(PsiElement enclosingElement, PsiElement selectionCandidate, TextRange selectedRange) {
        KotlinElementSelectionHandler handler = findHandler(enclosingElement);
        return handler.selectPrevious(enclosingElement, selectionCandidate, selectedRange);
    }
    
    private KotlinElementSelectionHandler findHandler(PsiElement enclosingElement) {
        for (KotlinElementSelectionHandler handler: selectionHandlers) {
            if (handler.canSelect(enclosingElement)) {
                return handler;
            }
        }
        return defaultHandler;        
    }
    
    private static class SingletonHolder {
        public static final KotlinElementSelectioner INSTANCE = new KotlinElementSelectioner();
    }
    
    public static KotlinElementSelectioner getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
