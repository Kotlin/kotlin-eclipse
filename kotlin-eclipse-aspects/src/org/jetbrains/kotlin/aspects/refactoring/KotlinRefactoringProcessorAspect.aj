package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;

public aspect KotlinRefactoringProcessorAspect {
    pointcut isApplicable() :
        args()
        && execution(boolean RefactoringProcessor.isApplicable());
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    boolean around() : isApplicable() {
        RefactoringProcessor refactoringProcessor = (RefactoringProcessor) thisJoinPoint.getTarget();
        for (Object element : refactoringProcessor.getElements()) {
            if (!(element instanceof IJavaElement)) return proceed();
            if (!EclipseJavaElementUtil.isKotlinLightClass((IJavaElement) element)) return proceed();
        }
        
        return true;
    }
}
