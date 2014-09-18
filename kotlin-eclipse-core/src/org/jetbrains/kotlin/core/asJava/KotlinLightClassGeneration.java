package org.jetbrains.kotlin.core.asJava;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.CompilationErrorHandler;
import org.jetbrains.jet.codegen.KotlinCodegenFacade;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

import com.intellij.openapi.project.Project;

public class KotlinLightClassGeneration {
    
    public static void buildAndSaveLightClasses(@NotNull AnalyzeExhaust analyzeExhaust, @NotNull IJavaProject javaProject) throws CoreException {
        KotlinJavaManager.INSTANCE.registerKtExternalBinFolder(javaProject);
        
        GenerationState state = buildLightClasses(
                analyzeExhaust, 
                javaProject,
                ProjectUtils.getSourceFiles(javaProject.getProject()));
        
        saveKotlinDeclarationClasses(state, javaProject);
    }
    
    public static GenerationState buildLightClasses(@NotNull AnalyzeExhaust analyzeExhaust, @NotNull IJavaProject javaProject, 
            @NotNull List<JetFile> jetFiles) {
        Project project = KotlinEnvironment.getEnvironment(javaProject).getProject();
        
        GenerationState state = new GenerationState(project, new LightClassBuilderFactory(), analyzeExhaust.getModuleDescriptor(),
                analyzeExhaust.getBindingContext(), jetFiles);
        
        KotlinCodegenFacade.compileCorrectFiles(state, new CompilationErrorHandler() {
            @Override
            public void reportException(Throwable exception, String fileUrl) {
                // skip
            }
        });
        
        return state;
    }
    
    private static void saveKotlinDeclarationClasses(@NotNull GenerationState state, @NotNull IJavaProject javaProject) throws CoreException {
        IProject project = javaProject.getProject();
        
        ProjectUtils.cleanFolder(KotlinJavaManager.INSTANCE.getKotlinBinFolderFor(project));
        
        for (OutputFile outputFile : state.getFactory().asList()) {
            IPath path = KotlinJavaManager.KOTLIN_BIN_FOLDER.append(new Path(outputFile.getRelativePath()));
            LightClassFile lightClassFile = new LightClassFile(project.getFile(path));
            
            createParentDirsFor(lightClassFile);
            lightClassFile.createNewFile();
            
            KotlinLightClassManager.INSTANCE.putClass(lightClassFile.asFile(), outputFile.getSourceFiles());
        }
    }
    
    private static void createParentDirsFor(@NotNull LightClassFile lightClassFile) {
        IFolder parent = (IFolder) lightClassFile.getResource().getParent();
        if (parent != null && !parent.exists()) {
            createParentDirs(parent);
        }
    }
    
    private static void createParentDirs(IFolder folder) {
        IContainer parent = folder.getParent();
        if (!parent.exists()) {
            createParentDirs((IFolder) parent);
        }
        
        try {
            folder.create(true, true, null);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
}
