package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;
import org.jetbrains.kotlin.ui.refactorings.rename.KotlinLightElementsFactory;


public aspect KotlinJavaDescriptorAspect {
    pointcut handleToElement(final WorkingCopyOwner owner, final String project, final String handle, final boolean check) :
        args(owner, project, handle, check)
        && execution(IJavaElement JavaRefactoringDescriptorUtil.handleToElement(WorkingCopyOwner, String, String, boolean));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    IJavaElement around(final WorkingCopyOwner owner, final String project, final String handle, final boolean check) : 
            handleToElement(owner, project, handle, check) {
        IJavaElement javaElement = proceed(owner, project, handle, check);
        if (EclipseJavaElementUtil.isKotlinLightClass(javaElement)) {
            return KotlinLightElementsFactory.createLightElement(javaElement);
        }
        return javaElement;
    }
}