/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResourceChangeEvent
import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.resolve.EclipseAnalyzerFacadeForJVM
import org.jetbrains.kotlin.core.utils.ProjectUtils
import java.util.concurrent.ConcurrentHashMap

object KotlinAnalysisProjectCache : IResourceChangeListener {
    private val cachedAnalysisResults = ConcurrentHashMap<IProject, AnalysisResult>()

    fun resetCache(project: IProject) {
        synchronized(project) {
            cachedAnalysisResults.remove(project)
        }
    }

    fun resetAllCaches() {
        cachedAnalysisResults.keys.toList().forEach {
            resetCache(it)
        }
    }

    fun getAnalysisResult(javaProject: IJavaProject): AnalysisResult {
        val project = javaProject.project
        return synchronized(project) {
            val analysisResult = cachedAnalysisResults.get(project) ?: run {
                EclipseAnalyzerFacadeForJVM.analyzeSources(
                        KotlinEnvironment.getEnvironment(project),
                        ProjectUtils.getSourceFiles(javaProject.project)).analysisResult
            }

            cachedAnalysisResults.putIfAbsent(project, analysisResult) ?: analysisResult
        }
    }

    @Synchronized
    public fun getAnalysisResultIfCached(project: IProject): AnalysisResult? {
        return cachedAnalysisResults.get(project)
    }

    override fun resourceChanged(event: IResourceChangeEvent) {
        when (event.type) {
            IResourceChangeEvent.PRE_DELETE,
            IResourceChangeEvent.PRE_CLOSE,
            IResourceChangeEvent.PRE_BUILD -> event.delta?.accept { delta ->
                val resource = delta.resource
                if (resource is IFile) {
                    val javaProject = JavaCore.create(resource.getProject())
                    if (KotlinPsiManager.isKotlinSourceFile(resource, javaProject)) {
                        cachedAnalysisResults.remove(resource.getProject())
                    }

                    return@accept false
                }

                true
            }
        }
    }
}