package org.jetbrains.kotlin.ui.launch;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;

public class KotlinLaunchableTester extends PropertyTester {
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (receiver instanceof IAdaptable) {
            IFile file = (IFile) ((IAdaptable) receiver).getAdapter(IFile.class);
            if (file != null) {
                JetFile jetFile = KotlinPsiManager.getKotlinParsedFile(file);
                if (jetFile == null) {
                    return false;
                }
                
                IJavaProject javaProject = JavaCore.create(file.getProject()); 
                BindingContext bindingContext = KotlinAnalyzer.analyzeDeclarations(javaProject).getBindingContext();
                return new MainFunctionDetector(bindingContext).hasMain(jetFile.getDeclarations());
            }
        }
        
        return false;
    }
}
