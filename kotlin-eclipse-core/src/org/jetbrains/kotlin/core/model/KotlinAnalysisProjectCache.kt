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
import kotlin.platform.platformStatic
import org.eclipse.core.resources.IProject
import java.util.concurrent.ConcurrentHashMap
import java.util.HashMap

public object KotlinAnalysisProjectCache {
    private val cachedAnalysisResults = HashMap<IProject, AnalysisResult>()
    
    public fun resetCache(javaProject: IJavaProject) {
        val project = javaProject.getProject()
        synchronized (project) {
            cachedAnalysisResults.remove(javaProject.getProject()) 
        }
    }
    
    public fun getAnalysisResult(javaProject: IJavaProject): AnalysisResult {
        val project = javaProject.getProject()
        synchronized (project) {
            return cachedAnalysisResults.getOrPut(project) {
                val environment = KotlinEnvironment.getEnvironment(javaProject)
                KotlinAnalyzer.analyzeFiles(javaProject, environment, ProjectUtils.getSourceFiles(javaProject.getProject()))
            }
        }
    }
}