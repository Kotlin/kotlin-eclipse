package org.jetbrains.kotlin.core.resolve;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.kotlin.core.builder.KotlinManager;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;

import com.google.common.base.Predicates;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class KotlinAnalyzer {
    
    private final Project ideaProject;
    private final JavaCoreApplicationEnvironment applicationEnvironment;

    public KotlinAnalyzer() {
        KotlinEnvironment kotlinEnvironment = new KotlinEnvironment();
        applicationEnvironment = kotlinEnvironment.getApplicationEnvironment();
        ideaProject = kotlinEnvironment.getProject();
    }

    public static BindingContext analyze() {
        return new KotlinAnalyzer().analyzeAllProjects();
    }

    public BindingContext analyzeAllProjects() {
        // TODO: Do not initialize builtins for each analyze
        KotlinBuiltIns.initialize(ideaProject);
        
        // TODO: No sdk and standard lib      
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                ideaProject, getJetFiles(), null, Predicates.<PsiFile>alwaysTrue());
        
        return analyzeExhaust.getBindingContext();
    }

    private List<JetFile> getJetFiles() {
        List<JetFile> jetFiles = new ArrayList<JetFile>();
        Set<IFile> files = new HashSet<IFile>(KotlinManager.getAllFiles());
        for (IFile file : files) {
            VirtualFile fileByPath = applicationEnvironment.getLocalFileSystem().findFileByPath(
                    file.getRawLocation().toOSString());
            jetFiles.add((JetFile) PsiManager.getInstance(ideaProject).findFile(fileByPath));
        }
        
        return jetFiles;
    }
}