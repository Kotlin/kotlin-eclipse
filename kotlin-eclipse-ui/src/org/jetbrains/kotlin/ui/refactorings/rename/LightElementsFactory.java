package org.jetbrains.kotlin.ui.refactorings.rename;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightElementsFactory {
    @Nullable
    public static IJavaElement getLightElement(@NotNull IJavaElement element) {
        if (element instanceof IType) {
            return new KotlinLightType((IType) element, null, null);
        } else if (element instanceof IMethod) {
            return new KotlinLightFunction((IMethod) element, null, null);
        } else {
            return null;
        }
    }
}
