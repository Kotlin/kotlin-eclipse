package org.jetbrains.kotlin.psi.visualization;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.intellij.lang.ASTNode;

public class PsiContentProvider implements ITreeContentProvider {

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        ASTNode element = (ASTNode) inputElement;
        return element.getChildren(null);
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        ASTNode element = (ASTNode) parentElement;
        return element.getChildren(null);
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (((ASTNode)element).getFirstChildNode() != null) {
            return true;
        } else {
            return false;
        }
    }

}
