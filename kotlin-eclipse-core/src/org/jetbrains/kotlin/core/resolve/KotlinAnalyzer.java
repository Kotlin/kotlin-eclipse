package org.jetbrains.kotlin.core.resolve;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;

import com.google.common.base.Predicates;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class KotlinAnalyzer {

    @NotNull
    public static BindingContext analyzeProject(@NotNull IJavaProject javaProject) {
        KotlinEnvironment kotlinEnvironment = new KotlinEnvironment(javaProject);
        
        // TODO: Do not initialize builtins for each analyze
        Project ideaProject = kotlinEnvironment.getProject();
        KotlinBuiltIns.initialize(ideaProject);
        
        JavaCoreApplicationEnvironment applicationEnvironment = KotlinEnvironment.getApplicationEnvironment();
        
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                ideaProject, getSourceFiles(javaProject, ideaProject, applicationEnvironment), null, Predicates.<PsiFile>alwaysTrue());
        
        return analyzeExhaust.getBindingContext();
    }
    
    @NotNull
    private static List<JetFile> getSourceFiles(@NotNull IJavaProject javaProject, @NotNull Project ideaProject, 
            @NotNull JavaCoreApplicationEnvironment applicationEnvironment) {
        List<JetFile> jetFiles = new ArrayList<JetFile>();
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(javaProject.getProject())) {
            String path = file.getRawLocation().toOSString();
            VirtualFile virtualFile = applicationEnvironment.getLocalFileSystem().findFileByPath(path);
            
            if (virtualFile != null) {
                jetFiles.add((JetFile) PsiManager.getInstance(ideaProject).findFile(virtualFile));
            }
        }
        
        return jetFiles;
    }
}