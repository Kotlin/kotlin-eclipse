package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.debug.core.refactoring.BreakpointChange;
import org.jetbrains.kotlin.ui.refactorings.rename.KotlinLightCompilationUnit;

@SuppressWarnings("restriction")
public aspect KotlinBreakpointRenamingAspect {
    pointcut findElement(IJavaElement parent, IJavaElement element): 
                args(parent, element) 
                && execution(IJavaElement BreakpointChange.findElement(IJavaElement, IJavaElement));

    @SuppressAjWarnings({"adviceDidNotMatch"})
    IJavaElement around(IJavaElement parent, IJavaElement element): findElement(parent, element) {
        if (parent instanceof KotlinLightCompilationUnit || element instanceof KotlinLightCompilationUnit) {
            return null;
        }
        
        return proceed(parent, element);
    }
}
