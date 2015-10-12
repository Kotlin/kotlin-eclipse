package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IJavaElement;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public aspect KotlinRenameMethodAvailabilityAspect {
    pointcut checkAvailability(IJavaElement javaElement) :
        args(javaElement)
        && execution(RefactoringStatus Checks.checkAvailability(IJavaElement));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    RefactoringStatus around(IJavaElement javaElement) : checkAvailability(javaElement) {
        if (EclipseJavaElementUtil.isKotlinLightClass(javaElement)) {
            return new RefactoringStatus();
        }
        
        return proceed(javaElement);
    }
}
