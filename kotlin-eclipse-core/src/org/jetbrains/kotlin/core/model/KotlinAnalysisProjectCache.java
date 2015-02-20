package org.jetbrains.kotlin.core.model;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

public class KotlinAnalysisProjectCache {
    private AnalysisResult cachedAnalysisResult = null;
    private final Object cacheLock = new Object();
    
    public void cacheAnalysisResult(@NotNull AnalysisResult analysisResult) {
        synchronized (cacheLock) {
            cachedAnalysisResult = analysisResult;
        }
    }
    
    public void resetCache() {
        synchronized (cacheLock) {
            cachedAnalysisResult = null;
        }
    }
    
    @Nullable
    public AnalysisResult getCachedAnalysisResult() {
        synchronized (cacheLock) {
            return cachedAnalysisResult;
        }
    }
    
    @NotNull
    public static KotlinAnalysisProjectCache getInstance(@NotNull IJavaProject javaProject) {
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        return ServiceManager.getService(ideaProject, KotlinAnalysisProjectCache.class);
    }
}
