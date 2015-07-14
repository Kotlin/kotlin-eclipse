package org.jetbrains.kotlin.ui.editors.selection;

import java.util.Iterator;

import com.intellij.psi.PsiElement;

public class PsiElementChildrenIterable implements Iterable<PsiElement> {
    
    private class PsiChildrenIterator implements Iterator<PsiElement> {
        
        private PsiElement currentElement;
        private final boolean reverse;
        
        public PsiChildrenIterator(PsiElement enclosingElement, boolean reverse) {
            if (reverse) {
                currentElement = enclosingElement.getLastChild();
            } else {
                currentElement = enclosingElement.getFirstChild();
            }
            this.reverse = reverse;
        }
        
        @Override
        public boolean hasNext() {
            return currentElement != null;
        }
        
        @Override
        public PsiElement next() {
            PsiElement result = currentElement;
            if (currentElement != null) {
                if (reverse) {
                    currentElement = currentElement.getPrevSibling();
                } else {
                    currentElement = currentElement.getNextSibling();
                }
            }
            return result;
        }
    }
    
    private final Iterator<PsiElement> iterator;
    
    private PsiElementChildrenIterable(PsiElement enclosingElement, boolean reverse) {
        iterator = new PsiChildrenIterator(enclosingElement, reverse);
    }
    
    @Override
    public Iterator<PsiElement> iterator() {
        return iterator;
    }
    
    public static PsiElementChildrenIterable forwardChildrenIterator(PsiElement enclosingElement) {
        return new PsiElementChildrenIterable(enclosingElement, true);
    }
    
    public static PsiElementChildrenIterable backwardChildrenIterator(PsiElement enclosingElement) {
        return new PsiElementChildrenIterable(enclosingElement, false);
    }
}
