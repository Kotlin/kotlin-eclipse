package org.jetbrains.kotlin.ui.editors.codeassist;

import org.jetbrains.annotations.Nullable;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.TreeUtil;

// These functions were copied from com.intellij.psi.filters.FilterPositionUtil
public class FilterPositionUtil {
    
    @Nullable
    public static PsiElement searchNonSpaceNonCommentBack(PsiElement element) {
        return searchNonSpaceNonCommentBack(element, false);
    }
    
    @Nullable
    public static PsiElement searchNonSpaceNonCommentBack(PsiElement element, final boolean strict) {
        if (element == null || element.getNode() == null)
            return null;
        ASTNode leftNeibour = TreeUtil.prevLeaf(element.getNode());
        if (!strict) {
            while (leftNeibour != null && (leftNeibour.getElementType() == TokenType.WHITE_SPACE
                    || leftNeibour.getPsi() instanceof PsiComment)) {
                leftNeibour = TreeUtil.prevLeaf(leftNeibour);
            }
        }
        return leftNeibour != null ? leftNeibour.getPsi() : null;
        
    }
}
