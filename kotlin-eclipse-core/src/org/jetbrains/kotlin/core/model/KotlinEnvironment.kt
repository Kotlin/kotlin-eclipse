/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory

val KT_JDK_ANNOTATIONS_PATH = ProjectUtils.buildLibPath("kotlin-jdk-annotations")
val KOTLIN_COMPILER_PATH = ProjectUtils.buildLibPath("kotlin-compiler")

class KotlinEnvironment private constructor(val javaProject: IJavaProject, disposable: Disposable) :
        KotlinCommonEnvironment(disposable) {
    init {
        registerProjectDependenServices(javaProject)
        configureClasspath(javaProject)
        
        cachedEnvironment.putEnvironment(javaProject, this)
    }
    
    private fun registerProjectDependenServices(javaProject: IJavaProject) {
        project.registerService(JvmVirtualFileFinderFactory::class.java, EclipseVirtualFileFinder(javaProject))
        project.registerService(KotlinLightClassManager::class.java, KotlinLightClassManager(javaProject.project))
    }
    
    private fun configureClasspath(javaProject: IJavaProject) {
        for (file in ProjectUtils.collectClasspathWithDependenciesForBuild(javaProject)) {
            addToClasspath(file)
        }
    }

    companion object {
        private val cachedEnvironment = CachedEnvironment()
        private val environmentCreation = {
            javaProject: IJavaProject -> KotlinEnvironment(javaProject, Disposer.newDisposable())
        }

        @JvmStatic fun getEnvironment(javaProject: IJavaProject): KotlinEnvironment {
            return cachedEnvironment.getOrCreateEnvironment(javaProject, environmentCreation)
        }

        @JvmStatic fun updateKotlinEnvironment(javaProject: IJavaProject) {
            cachedEnvironment.updateEnvironment(javaProject, environmentCreation)
        }

        @JvmStatic fun getJavaProject(project: Project): IJavaProject? = cachedEnvironment.getJavaProject(project)
    }
}