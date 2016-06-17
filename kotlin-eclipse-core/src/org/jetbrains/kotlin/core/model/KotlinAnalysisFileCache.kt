package org.jetbrains.kotlin.core.model

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.eclipse.core.resources.IFile
import java.util.LinkedHashMap
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider
import org.jetbrains.kotlin.core.resolve.EclipseAnalyzerFacadeForJVM
<<<<<<< HEAD
=======
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
>>>>>>> 9cc72ba... Generalize api to analyze files with respect to scripts

data class FileAnalysisResults(val file: KtFile, val analysisResult: AnalysisResultWithProvider)

public object KotlinAnalysisFileCache {
    private @Volatile var lastAnalysedFileCache: FileAnalysisResults? = null
<<<<<<< HEAD
    
    public @Synchronized fun getAnalysisResult(file: KtFile, project: IJavaProject): AnalysisResultWithProvider {
        return if (lastAnalysedFileCache != null && lastAnalysedFileCache!!.file == file) {
            lastAnalysedFileCache!!.analysisResult
        } else {
            val kotlinEnvironment = KotlinEnvironment.getEnvironment(project)
            val analysisResult = EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
<<<<<<< HEAD
                project, 
                kotlinEnvironment.getProject(), 
                listOf(file))
=======
                    KotlinEnvironment.getEnvironment(project.project),
                    listOf(file))
>>>>>>> 4e6f838... Replace IJavaProject with raw IProject in several places
=======

    public @Synchronized fun getAnalysisResult(file: KtFile): AnalysisResultWithProvider {
        return if (lastAnalysedFileCache != null && lastAnalysedFileCache!!.file == file) {
            lastAnalysedFileCache!!.analysisResult
        } else {
            val eclipseFile = KotlinPsiManager.getEclipseFile(file)!!
            val environment = getEnvironment(eclipseFile)
            
            val analysisResult = when (environment) {
                is KotlinScriptEnvironment -> EclipseAnalyzerFacadeForJVM.analyzeScript(environment, file)
                is KotlinEnvironment -> EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(environment, listOf(file))
                else -> throw IllegalArgumentException("Could not analyze file with environment: $environment")
            }
            
>>>>>>> 9cc72ba... Generalize api to analyze files with respect to scripts
            lastAnalysedFileCache = FileAnalysisResults(file, analysisResult)
            lastAnalysedFileCache!!.analysisResult
        }
    }
    
    public fun resetCache() {
        lastAnalysedFileCache = null
    }
}