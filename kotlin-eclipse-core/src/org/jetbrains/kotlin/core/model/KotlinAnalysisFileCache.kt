package org.jetbrains.kotlin.core.model

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.eclipse.core.resources.IFile
import java.util.LinkedHashMap
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider

data class FileAnalysisResults(val file: JetFile?, val analysisResult: AnalysisResultWithProvider?) {
    companion object {
        val EMPTY = FileAnalysisResults(null, null) 
    }
}

public object KotlinAnalysisFileCache {
    private var lastAnalysedFileCache = FileAnalysisResults.EMPTY
    
    public @Synchronized fun getAnalysisResult(file: JetFile, project: IJavaProject): AnalysisResultWithProvider {
        if (lastAnalysedFileCache.file == file && lastAnalysedFileCache.analysisResult != null) {
            return lastAnalysedFileCache.analysisResult!!
        } else {
            lastAnalysedFileCache = FileAnalysisResults(file, KotlinAnalyzer.analyzeFiles(project, listOf(file)))
            return lastAnalysedFileCache.analysisResult!!
        }
    }
    
    public fun resetCache() {
        lastAnalysedFileCache = FileAnalysisResults.EMPTY
    }
}