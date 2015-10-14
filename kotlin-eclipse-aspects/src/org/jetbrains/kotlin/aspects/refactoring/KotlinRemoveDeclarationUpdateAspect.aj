package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameNonVirtualMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;

@SuppressWarnings("restriction")
public aspect KotlinRemoveDeclarationUpdateAspect {
    pointcut addDeclarationUpdate(TextChangeManager manager) :
        args(manager)
        && execution(void RenameNonVirtualMethodProcessor.addDeclarationUpdate(TextChangeManager));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    void around(TextChangeManager manager) : addDeclarationUpdate(manager) {
        RenameNonVirtualMethodProcessor processor = (RenameNonVirtualMethodProcessor) thisJoinPoint.getTarget();
        if (EclipseJavaElementUtil.isKotlinLightClass(processor.getMethod())) {
            return;
        }
        
        proceed(manager);
    }
}
