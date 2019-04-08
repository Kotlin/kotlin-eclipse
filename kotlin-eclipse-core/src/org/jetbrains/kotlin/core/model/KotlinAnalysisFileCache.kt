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

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider
import org.jetbrains.kotlin.core.resolve.EclipseAnalyzerFacadeForJVM
import org.jetbrains.kotlin.core.resolve.KotlinCoroutinesScope
import org.jetbrains.kotlin.psi.KtFile

data class FileAnalysisResults(val file: KtFile, val analysisResult: Deferred<AnalysisResultWithProvider>)

object KotlinAnalysisFileCache {

    @Volatile
    private var lastAnalysedFileCache: FileAnalysisResults? = null

    fun getAnalysisResult(file: KtFile): AnalysisResultWithProvider {
        return runBlocking {
            getImmediatelyFromCache(file)?.await()
        } ?: runBlocking {
            val environment = getEnvironment(file.project)!!
            val analysisResult = KotlinCoroutinesScope.async {
                resolve(file, environment)
            }
            saveLastResult(file, analysisResult).analysisResult.await()
        }
    }

    @Synchronized
    private fun saveLastResult(file: KtFile, result: Deferred<AnalysisResultWithProvider>): FileAnalysisResults =
        FileAnalysisResults(file, result).also {
            lastAnalysedFileCache = it
        }
    
    fun resetCache() {
        lastAnalysedFileCache = null
    }
    
    private fun resolve(file: KtFile, environment: KotlinCommonEnvironment): AnalysisResultWithProvider =
        when (environment) {
            is KotlinScriptEnvironment -> EclipseAnalyzerFacadeForJVM.analyzeScript(environment, file)
            is KotlinEnvironment -> EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(environment, listOf(file))
            else -> throw IllegalArgumentException("Could not analyze file with environment: $environment")
        }
    
    @Synchronized
    private fun getImmediatelyFromCache (ktFile: KtFile) : Deferred<AnalysisResultWithProvider>? =
        lastAnalysedFileCache?.takeIf { it.file == ktFile }?.analysisResult
}