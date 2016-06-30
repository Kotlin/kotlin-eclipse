/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.model

import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider
import org.jetbrains.kotlin.core.resolve.EclipseAnalyzerFacadeForJVM
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager

data class FileAnalysisResults(val file: KtFile, val analysisResult: AnalysisResultWithProvider)

public object KotlinAnalysisFileCache {
    private @Volatile var lastAnalysedFileCache: FileAnalysisResults? = null

    @Synchronized fun getAnalysisResult(file: KtFile): AnalysisResultWithProvider {
        return getImmediatlyFromCache(file) ?: run {
            val environment = getEnvironment(file.project)!!
            val analysisResult = resolve(file, environment)
            
            lastAnalysedFileCache = FileAnalysisResults(file, analysisResult)
            lastAnalysedFileCache!!.analysisResult
        }
    }
    
    fun resetCache() {
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