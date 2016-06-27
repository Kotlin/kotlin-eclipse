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
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.asJava.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JavaRoot
import org.jetbrains.kotlin.cli.jvm.compiler.JvmCliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.JvmDependenciesIndex
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import java.util.ArrayList
import org.eclipse.core.resources.IResource

val KOTLIN_COMPILER_PATH = ProjectUtils.buildLibPath("kotlin-compiler")

fun getEnvironment(eclipseFile: IFile): KotlinCommonEnvironment {
    return if (KotlinScriptEnvironment.isScript(eclipseFile)) {
        KotlinScriptEnvironment.getEnvironment(eclipseFile)
    } else {
        KotlinEnvironment.getEnvironment(eclipseFile.project)
    }
}

fun getEclipseResource(ideaProject: Project): IResource? {
    val project = KotlinEnvironment.getJavaProject(ideaProject)
    if (project != null) {
        return project
    }
    
    return KotlinScriptEnvironment.getEclipseFile(ideaProject)
}

class KotlinScriptEnvironment private constructor(val eclipseFile: IFile, disposalbe: Disposable) :
        KotlinCommonEnvironment(disposalbe) {
    init {
        configureClasspath()
        
        project.registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(project))
        
        val kotlinRuntimeRoot = JavaRoot(getRoots().first(), JavaRoot.RootType.BINARY)
        val index = JvmDependenciesIndex(listOf(kotlinRuntimeRoot))
        
        project.registerService(JvmVirtualFileFinderFactory::class.java, JvmCliVirtualFileFinderFactory(index))
    }
    
    companion object {
        private val kotlinRuntimePath = Path(ProjectUtils.buildLibPath(KotlinClasspathContainer.LIB_RUNTIME_NAME))
        
        private val cachedEnvironment = CachedEnvironment<IFile, KotlinScriptEnvironment>()
        private val environmentCreation = {
            eclipseFile: IFile -> KotlinScriptEnvironment(eclipseFile, Disposer.newDisposable())
        }

        @JvmStatic fun getEnvironment(file: IFile): KotlinScriptEnvironment {
            checkIsScript(file)
            
            return cachedEnvironment.getOrCreateEnvironment(file, environmentCreation)
        }

        @JvmStatic fun updateKotlinEnvironment(file: IFile) {
            checkIsScript(file)
            
            cachedEnvironment.updateEnvironment(file, environmentCreation)
        }
        
        @JvmStatic fun removeKotlinEnvironment(file: IFile) {
            checkIsScript(file)
            
            cachedEnvironment.removeEnvironment(file)
        }
        
        @JvmStatic fun getEclipseFile(project: Project): IFile? = cachedEnvironment.getEclipseResource(project)
        
        fun isScript(file: IFile): Boolean {
            return file.fileExtension == KotlinParserDefinition.STD_SCRIPT_SUFFIX // TODO: use ScriptDefinitionProvider
        }
        
        private fun checkIsScript(file: IFile) {
            if (!isScript(file)) {
                throw IllegalArgumentException("KotlinScriptEnvironment can work only with scripts, not ${file.name}")
            }
        }
    }
    
    private fun configureClasspath() {
        addToClasspath(kotlinRuntimePath.toFile())
    }
}

class KotlinEnvironment private constructor(val eclipseProject: IProject, disposable: Disposable) :
        KotlinCommonEnvironment(disposable) {
    val javaProject = JavaCore.create(eclipseProject)
    
    init {
        registerProjectDependenServices(javaProject)
        configureClasspath(javaProject)
        
        val cliLightClassGenerationSupport = CliLightClassGenerationSupport(project)
        with(project) {
            registerService(LightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
            registerService(CliLightClassGenerationSupport::class.java, cliLightClassGenerationSupport)
            registerService(CodeAnalyzerInitializer::class.java, cliLightClassGenerationSupport)
            
            registerService(KtLightClassForFacade.FacadeStubCache::class.java, KtLightClassForFacade.FacadeStubCache(project))
        }
        
        cachedEnvironment.putEnvironment(eclipseProject, this)
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
        private val cachedEnvironment = CachedEnvironment<IProject, KotlinEnvironment>()
        private val environmentCreation = {
            eclipseProject: IProject -> KotlinEnvironment(eclipseProject, Disposer.newDisposable())
        }

        @JvmStatic
        fun getEnvironment(eclipseProject: IProject): KotlinEnvironment {
            return cachedEnvironment.getOrCreateEnvironment(eclipseProject, environmentCreation)
        }

        @JvmStatic fun updateKotlinEnvironment(eclipseProject: IProject) {
            cachedEnvironment.updateEnvironment(eclipseProject, environmentCreation)
        }

        @JvmStatic fun getJavaProject(project: Project): IProject? = cachedEnvironment.getEclipseResource(project)
    }
}