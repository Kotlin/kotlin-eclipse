package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;

@SuppressWarnings("restriction")
public aspect KotlinRefactoringTypeAspect {
    pointcut addTypeDeclarationUpdate(TextChangeManager manager) :
        args(manager)
        && execution(void RenameTypeProcessor.addTypeDeclarationUpdate(TextChangeManager));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    void around(TextChangeManager manager) : addTypeDeclarationUpdate(manager) {
        RenameTypeProcessor processor = (RenameTypeProcessor) thisJoinPoint.getTarget();
        if (!EclipseJavaElementUtil.isKotlinLightClass(processor.getType())) {
            proceed(manager);
        }
    }
}
