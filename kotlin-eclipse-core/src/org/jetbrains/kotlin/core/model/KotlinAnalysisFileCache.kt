package org.jetbrains.kotlin.core.model

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.eclipse.core.resources.IFile
import java.util.LinkedHashMap
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider
import org.jetbrains.kotlin.core.resolve.EclipseAnalyzerFacadeForJVM

data class FileAnalysisResults(val file: KtFile, val analysisResult: AnalysisResultWithProvider)

public object KotlinAnalysisFileCache {
    private @Volatile var lastAnalysedFileCache: FileAnalysisResults? = null
    
    public @Synchronized fun getAnalysisResult(file: KtFile, project: IJavaProject): AnalysisResultWithProvider {
        return if (lastAnalysedFileCache != null && lastAnalysedFileCache!!.file == file) {
            lastAnalysedFileCache!!.analysisResult
        } else {
            val kotlinEnvironment = KotlinEnvironment.getEnvironment(project)
            val analysisResult = EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project, 
                kotlinEnvironment.getProject(), 
                listOf(file))
            lastAnalysedFileCache = FileAnalysisResults(file, analysisResult)
            lastAnalysedFileCache!!.analysisResult
        }
    }
    
    public fun resetCache() {
        lastAnalysedFileCache = null
    }
}