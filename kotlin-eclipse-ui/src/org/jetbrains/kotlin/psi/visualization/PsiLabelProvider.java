package org.jetbrains.kotlin.psi.visualization;

import org.eclipse.jface.viewers.LabelProvider;

import com.intellij.lang.ASTNode;

public class PsiLabelProvider extends LabelProvider {
    
    @Override
    public String getText(Object element) {
        if (element instanceof ASTNode) {
            return element.toString();
        }
        return "Null"; // It will never happen
    }

}
