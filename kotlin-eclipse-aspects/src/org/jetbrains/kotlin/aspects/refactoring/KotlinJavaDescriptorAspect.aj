package org.jetbrains.kotlin.aspects.refactoring;

import java.util.List;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IEditorPart;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor;
import org.jetbrains.kotlin.ui.refactorings.rename.KotlinLightElementsFactory;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptorUtil;

public aspect KotlinJavaDescriptorAspect {
    pointcut handleToElement(final WorkingCopyOwner owner, final String project, final String handle, final boolean check) :
        args(owner, project, handle, check)
        && execution(IJavaElement JavaRefactoringDescriptorUtil.handleToElement(WorkingCopyOwner, String, String, boolean));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    IJavaElement around(final WorkingCopyOwner owner, final String project, final String handle, final boolean check) : 
            handleToElement(owner, project, handle, check) {
        IJavaElement javaElement = proceed(owner, project, handle, check);
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