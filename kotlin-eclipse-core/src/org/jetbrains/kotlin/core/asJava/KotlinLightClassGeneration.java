package org.jetbrains.kotlin.core.asJava;

import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.codegen.CompilationErrorHandler;
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.psi.JetFile;

import com.intellij.openapi.project.Project;

public class KotlinLightClassGeneration {
    
    public static void updateLightClasses(
            @NotNull AnalysisResult analysisResult, 
            @NotNull IJavaProject javaProject,
            @NotNull Set<IFile> affectedFiles) throws CoreException {
        if (!KotlinJavaManager.INSTANCE$.hasLinkedKotlinBinFolder(javaProject)) {
            return;
        }
        
        KotlinLightClassManager.getInstance(javaProject).computeLightClassesSources();
        KotlinLightClassManager.getInstance(javaProject).updateLightClasses(affectedFiles);
    }
    
    public static GenerationState buildLightClasses(@NotNull AnalysisResult analysisResult, @NotNull IJavaProject javaProject, 
            @NotNull List<JetFile> jetFiles) {
        Project project = KotlinEnvironment.getEnvironment(javaProject).getProject();
        
        GenerationState state = new GenerationState(
                project, 
                new LightClassBuilderFactory(), 
                analysisResult.getModuleDescriptor(),
                analysisResult.getBindingContext(), 
                jetFiles);
        
        KotlinCodegenFacade.compileCorrectFiles(state, new CompilationErrorHandler() {
            @Override
            public void reportException(Throwable exception, String fileUrl) {
                // skip
            }
        });
        
        return state;
    }
}
