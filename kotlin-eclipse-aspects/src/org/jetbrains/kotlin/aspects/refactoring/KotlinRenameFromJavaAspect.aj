package org.jetbrains.kotlin.aspects.refactoring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.ui.refactoring.actions.RenameJavaElementAction;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor;
import org.jetbrains.kotlin.ui.refactorings.rename.KotlinLightElementsFactory;

public aspect KotlinRenameFromJavaAspect {
    pointcut getJavaElementFromEditor() :
        args()
        && execution(IJavaElement RenameJavaElementAction.getJavaElementFromEditor());
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    IJavaElement around() : getJavaElementFromEditor() {
        IJavaElement javaElement = proceed();
        if (EclipseJavaElementUtil.isKotlinLightClass(javaElement)) {
            List<KtFile> sourceFiles = KotlinOpenEditor.findSourceFiles(javaElement);
            if (sourceFiles.size() == 1) {
                IFile eclipseFile = KotlinPsiManager.getEclispeFile(sourceFiles.get(0));
                return KotlinLightElementsFactory.createLightElement(javaElement, eclipseFile);
            }
        }
        return javaElement;
    }
}
