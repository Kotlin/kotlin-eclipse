package org.jetbrains.kotlin.core.model

import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.core.utils.ProjectUtils
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import kotlin.platform.platformStatic

public class KotlinAnalysisProjectCache(val javaProject: IJavaProject) {
    companion object {
        @platformStatic
        public fun getInstance(javaProject: IJavaProject): KotlinAnalysisProjectCache {
            val ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject()
            return ServiceManager.getService(ideaProject, javaClass<KotlinAnalysisProjectCache>())
        }
    }
    
    private var cachedAnalysisResult: AnalysisResult? = null
    private val cacheLock = Object()
    
    public fun resetCache() {
        synchronized (cacheLock) {
            cachedAnalysisResult = null
        }
    }
    
    public fun getAnalysisResult(): AnalysisResult {
        synchronized (cacheLock) {
            if (cachedAnalysisResult == null) {
                val environment = KotlinEnvironment.getEnvironment(javaProject)
                cachedAnalysisResult = KotlinAnalyzer.analyzeFiles(javaProject, environment, ProjectUtils.getSourceFiles(javaProject.getProject()))
            }
                
            return cachedAnalysisResult!!
        }
    }
}