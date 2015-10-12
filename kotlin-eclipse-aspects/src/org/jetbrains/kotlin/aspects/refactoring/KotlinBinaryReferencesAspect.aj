package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;

@SuppressWarnings("restriction")
public aspect KotlinBinaryReferencesAspect {
    pointcut addErrorIfNecessary(RefactoringStatus status) :
        args(status)
        && execution(void ReferencesInBinaryContext.addErrorIfNecessary(RefactoringStatus));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    void around(RefactoringStatus status) : addErrorIfNecessary(status) {
        ReferencesInBinaryContext targetObject = (ReferencesInBinaryContext) thisJoinPoint.getTarget();
        boolean hasKotlinReference = false;
        for (SearchMatch match : targetObject.getMatches()) {
            Object element = match.getElement();
            if (element instanceof IJavaElement && EclipseJavaElementUtil.isKotlinLightClass((IJavaElement) element)) {
                hasKotlinReference = true;
                break;
            }
        }
        
        if (!hasKotlinReference) {
            proceed(status);
        }
    }
}
