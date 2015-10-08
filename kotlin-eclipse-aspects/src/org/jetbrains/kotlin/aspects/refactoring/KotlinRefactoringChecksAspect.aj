package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;

public aspect KotlinRefactoringChecksAspect {
    pointcut checkIfCuBroken(IMember member) :
        args(member)
        && execution(RefactoringStatus Checks.checkIfCuBroken(IMember));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    RefactoringStatus around(IMember member) : checkIfCuBroken(member) {
        if (EclipseJavaElementUtil.isKotlinLightClass(member)) {
            return new RefactoringStatus();
        }
        
        return proceed(member);
    }
}