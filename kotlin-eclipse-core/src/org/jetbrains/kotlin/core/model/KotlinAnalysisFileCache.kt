package org.jetbrains.kotlin.core.model

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.eclipse.core.resources.IFile
import java.util.LinkedHashMap
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider

data class FileAnalysisResults(val file: JetFile, val analysisResult: AnalysisResultWithProvider)

public object KotlinAnalysisFileCache {
    private @Volatile var lastAnalysedFileCache: FileAnalysisResults? = null
    
    public @Synchronized fun getAnalysisResult(file: JetFile, project: IJavaProject): AnalysisResultWithProvider {
        return if (lastAnalysedFileCache != null && lastAnalysedFileCache!!.file == file) {
            lastAnalysedFileCache!!.analysisResult
        } else {
            lastAnalysedFileCache = FileAnalysisResults(file, KotlinAnalyzer.analyzeFiles(project, listOf(file)))
            lastAnalysedFileCache!!.analysisResult
        }
    }
    
    public fun resetCache() {
        lastAnalysedFileCache = null
    }
}