package org.jetbrains.kotlin.ui.refactorings.rename;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KotlinLightElementsFactory {
    @Nullable
    public static IJavaElement createLightElement(@NotNull IJavaElement javaElement) {
        if (javaElement instanceof IType) {
            return new KotlinLightType((IType) javaElement);
        } else if (javaElement instanceof IMethod) {
            return new KotlinLightFunction((IMethod) javaElement);
        } else {
            return null;
        }
    }
}
