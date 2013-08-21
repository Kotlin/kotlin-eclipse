package org.jetbrains.kotlin.core.resolve;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

public class KotlinAnalyzer {

    @NotNull
    public static BindingContext analyzeProject(@NotNull IJavaProject javaProject) {
        KotlinEnvironment.updateKotlinEnvironment(javaProject);
        KotlinEnvironment kotlinEnvironment = KotlinEnvironment.getEnvironment(javaProject);
        return analyzeProject(javaProject, kotlinEnvironment);
    }
    
    @NotNull
    private static BindingContext analyzeProject(@NotNull IJavaProject javaProject, @NotNull KotlinEnvironment kotlinEnvironment) {
        // TODO: Do not initialize builtins for each analyze
        Project ideaProject = kotlinEnvironment.getProject();
        KotlinBuiltIns.initialize(ideaProject);
        
        List<JetFile> sourceFiles = getSourceFiles(javaProject.getProject());
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                ideaProject, sourceFiles, null, Predicates.<PsiFile>alwaysTrue());
        
        return analyzeExhaust.getBindingContext();
    }
    
    @NotNull
    public static List<JetFile> getSourceFiles(@NotNull IProject project) {
        List<JetFile> jetFiles = new ArrayList<JetFile>();
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
            jetFiles.add(jetFile);
         }
        
        return jetFiles;
    }
}