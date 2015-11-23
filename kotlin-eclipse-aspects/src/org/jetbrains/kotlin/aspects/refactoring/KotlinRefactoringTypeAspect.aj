package org.jetbrains.kotlin.aspects.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;

@SuppressWarnings("restriction")
public aspect KotlinRefactoringTypeAspect {
    pointcut addTypeDeclarationUpdate(TextChangeManager manager) :
        args(manager)
        && execution(void RenameTypeProcessor.addTypeDeclarationUpdate(TextChangeManager));
    
    pointcut checkImportedTypes() :
        args()
        && execution(RefactoringStatus RenameTypeProcessor.checkImportedTypes());
    
    // Prohibit renaming Kotlin type declaration from JDT
    @SuppressAjWarnings({"adviceDidNotMatch"})
    void around(TextChangeManager manager) : addTypeDeclarationUpdate(manager) {
        try {
            Method method = RenameTypeProcessor.class.getDeclaredMethod("getType");
            IType type = (IType) method.invoke(thisJoinPoint.getTarget());
            if (!EclipseJavaElementUtil.isKotlinLightClass(type)) {
                proceed(manager);
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | 
                IllegalArgumentException | InvocationTargetException e) {
            // skip
        }
    }
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    RefactoringStatus around() : checkImportedTypes() {
        try {
            Method method = RenameTypeProcessor.class.getDeclaredMethod("getType");
            IType type = (IType) method.invoke(thisJoinPoint.getTarget());
            if (EclipseJavaElementUtil.isKotlinLightClass(type)) {
                return new RefactoringStatus();
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | 
                IllegalArgumentException | InvocationTargetException e) {
            // skip
        }
        
        return proceed();
    }
}