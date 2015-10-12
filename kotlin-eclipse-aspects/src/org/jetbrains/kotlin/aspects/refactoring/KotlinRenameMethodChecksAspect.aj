package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;

public aspect KotlinRenameMethodChecksAspect {
    pointcut isAvailable(IJavaElement javaElement) :
        args(javaElement)
        && execution(boolean Checks.isAvailable(IJavaElement));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    boolean around(IJavaElement javaElement) : isAvailable(javaElement) {
        if (EclipseJavaElementUtil.isKotlinLightClass(javaElement)) {
            return true;
        }
        
        return proceed(javaElement);
    }
}
