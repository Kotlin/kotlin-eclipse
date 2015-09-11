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

import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.core.utils.ProjectUtils
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.eclipse.core.resources.IProject
import java.util.concurrent.ConcurrentHashMap
import org.eclipse.core.resources.IResourceChangeListener
import org.eclipse.core.resources.IResourceChangeEvent
import org.jetbrains.kotlin.core.resolve.AnalysisResultWithProvider
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.builder.KotlinPsiManager

public object KotlinAnalysisProjectCache : IResourceChangeListener {
    private val cachedAnalysisResults = ConcurrentHashMap<IProject, AnalysisResult>()
    
    public fun resetCache(javaProject: IJavaProject) {
        val project = javaProject.getProject()
        synchronized(project) {
            cachedAnalysisResults.remove(javaProject.getProject())
        }
    }
    
    public fun getAnalysisResult(javaProject: IJavaProject): AnalysisResult {
        val project = javaProject.getProject()
        return synchronized(project) {
            val analysisResult = cachedAnalysisResults.getOrElse(project) {
                val environment = KotlinEnvironment.getEnvironment(javaProject)
                KotlinAnalyzer
                        .analyzeFiles(javaProject, environment, ProjectUtils.getSourceFiles(javaProject.getProject()))
                        .analysisResult
            }
            
            cachedAnalysisResults.putIfAbsent(project, analysisResult) ?: analysisResult
        }
    }
    
    public @Synchronized fun getAnalysisResultIfCached(javaProject: IJavaProject): AnalysisResult? {
        return cachedAnalysisResults.get(javaProject.getProject())
    }
    
    override fun resourceChanged(event: IResourceChangeEvent) {
        when (event.getType()) {
            IResourceChangeEvent.PRE_DELETE,
            IResourceChangeEvent.PRE_CLOSE,
            IResourceChangeEvent.PRE_BUILD -> event.getDelta()?.accept { delta ->
                val resource = delta.getResource()
                if (resource is IFile){
                    val javaProject = JavaCore.create(resource.getProject())
                    if (KotlinPsiManager.INSTANCE.isKotlinSourceFile(resource, javaProject)) {
                        cachedAnalysisResults.remove(resource.getProject())
                    }
                    
                    return@accept false
                }
                
                true
            }
        }
    }
}