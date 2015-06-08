package org.jetbrains.kotlin.core.model;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

public class KotlinAnalysisProjectCache {
    private final IJavaProject javaProject;
    
    private AnalysisResult cachedAnalysisResult = null;
    private final Object cacheLock = new Object();
    
    public KotlinAnalysisProjectCache(@NotNull IJavaProject javaProject) {
        this.javaProject = javaProject;
    }
    
    public void resetCache() {
        synchronized (cacheLock) {
            cachedAnalysisResult = null;
        }
    }
    
    @NotNull
    public AnalysisResult getAnalysisResult() {
        synchronized (cacheLock) {
            if (cachedAnalysisResult == null) {
                KotlinEnvironment environment = KotlinEnvironment.getEnvironment(javaProject);
                cachedAnalysisResult = KotlinAnalyzer.analyzeFiles(javaProject, environment, ProjectUtils.getSourceFiles(javaProject.getProject()));
            }
            
            return cachedAnalysisResult;
        }
    }
    
    @NotNull
    public static KotlinAnalysisProjectCache getInstance(@NotNull IJavaProject javaProject) {
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        return ServiceManager.getService(ideaProject, KotlinAnalysisProjectCache.class);
    }
}
