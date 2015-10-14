package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.refactoring.actions.RenameJavaElementAction;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;
import org.jetbrains.kotlin.ui.refactorings.rename.LightElementsFactory;

@SuppressWarnings("restriction")
public aspect KotlinRenameJavaElementAspect {
    pointcut getJavaElementFromEditor() :
        args()
        && execution(IJavaElement RenameJavaElementAction.getJavaElementFromEditor());
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    IJavaElement around() : getJavaElementFromEditor() {
        IJavaElement javaElement = proceed();
        if (EclipseJavaElementUtil.isKotlinLightClass(javaElement)) {
            IJavaElement kotlinElement = LightElementsFactory.getLightElement(javaElement);
            if (kotlinElement != null) {
                return kotlinElement;
            }
        }
        
        return javaElement;
    }
}
