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

    // This method can work only with real files that are present in VFS
    @Synchronized fun getAnalysisResult(file: KtFile): AnalysisResultWithProvider {
        return getImmediatlyFromCache(file) ?: run {
            val eclipseFile = KotlinPsiManager.getEclipseFile(file)!!
            val environment = getEnvironment(eclipseFile)
            
            getAnalysisResult(file, environment)
        }
    }
    
    // This method can take synthetic files
    @Synchronized fun getAnalysisResult(file: KtFile, environment: KotlinCommonEnvironment): AnalysisResultWithProvider {
        return getImmediatlyFromCache(file) ?: run {
            val analysisResult = resolve(file, environment)
            
>>>>>>> 9cc72ba... Generalize api to analyze files with respect to scripts
            lastAnalysedFileCache = FileAnalysisResults(file, analysisResult)
            lastAnalysedFileCache!!.analysisResult
        }
    }
    
<<<<<<< HEAD
    public fun resetCache() {
=======
    fun resetCache() {
>>>>>>> c00d89f... Fix organize imports for scripts: generalize KotlinCacheService
        lastAnalysedFileCache = null
    }
    
    private fun resolve(file: KtFile, environment: KotlinCommonEnvironment): AnalysisResultWithProvider {
        return when (environment) {
            is KotlinScriptEnvironment -> EclipseAnalyzerFacadeForJVM.analyzeScript(environment, file)
            is KotlinEnvironment -> EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(environment, listOf(file))
            else -> throw IllegalArgumentException("Could not analyze file with environment: $environment")
        }
    }
    
    @Synchronized
    private fun getImmediatlyFromCache(file: KtFile): AnalysisResultWithProvider? {
        return if (lastAnalysedFileCache != null && lastAnalysedFileCache!!.file == file)
            lastAnalysedFileCache!!.analysisResult
        else
            null
    }
}