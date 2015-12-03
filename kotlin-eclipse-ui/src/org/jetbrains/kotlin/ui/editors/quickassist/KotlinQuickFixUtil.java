package org.jetbrains.kotlin.ui.editors.quickassist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFunctionLiteral;
import org.jetbrains.kotlin.psi.KtIfExpression;
import org.jetbrains.kotlin.psi.KtParenthesizedExpression;
import org.jetbrains.kotlin.psi.KtReturnExpression;
import org.jetbrains.kotlin.psi.KtWithExpressionInitializer;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

// Copied from org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
public class KotlinQuickFixUtil {
    public static boolean canFunctionOrGetterReturnExpression(@NotNull KtDeclaration functionOrGetter, @NotNull KtExpression expression) {
        if (functionOrGetter instanceof KtFunctionLiteral) {
            KtBlockExpression functionLiteralBody = ((KtFunctionLiteral) functionOrGetter).getBodyExpression();
            PsiElement returnedElement = functionLiteralBody == null ? null : functionLiteralBody.getLastChild();
            return returnedElement instanceof KtExpression && canEvaluateTo((KtExpression) returnedElement, expression);
        }
        else {
            if (functionOrGetter instanceof KtWithExpressionInitializer && canEvaluateTo(((KtWithExpressionInitializer) functionOrGetter).getInitializer(), expression)) {
                return true;
            }
            KtReturnExpression returnExpression = PsiTreeUtil.getParentOfType(expression, KtReturnExpression.class);
            return returnExpression != null && canEvaluateTo(returnExpression.getReturnedExpression(), expression);
        }
    }
    
    public static boolean canEvaluateTo(KtExpression parent, KtExpression child) {
        if (parent == null || child == null) {
            return false;
        }
        while (parent != child) {
            if (child.getParent() instanceof KtParenthesizedExpression) {
                child = (KtExpression) child.getParent();
                continue;
            }
            child = getParentIfForBranch(child);
            if (child == null) return false;
        }
        return true;
    }
    
    @Nullable
    public static KtIfExpression getParentIfForBranch(@Nullable KtExpression expression) {
        KtIfExpression ifExpression = PsiTreeUtil.getParentOfType(expression, KtIfExpression.class, true);
        if (ifExpression == null) return null;
        if (equalOrLastInThenOrElse(ifExpression.getThen(), expression)
            || equalOrLastInThenOrElse(ifExpression.getElse(), expression)) {
            return ifExpression;
        }
        return null;
    }
    
    private static boolean equalOrLastInThenOrElse(KtExpression thenOrElse, KtExpression expression) {
        if (thenOrElse == expression) return true;
        return thenOrElse instanceof KtBlockExpression && expression.getParent() == thenOrElse &&
               PsiTreeUtil.getNextSiblingOfType(expression, KtExpression.class) == null;
    }
}
