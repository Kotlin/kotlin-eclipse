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

import com.intellij.core.CoreJavaFileManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.impl.PsiElementFinderImpl
import com.intellij.psi.impl.file.impl.JavaFileManager
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IClasspathContainer
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.core.JavaProject
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.KtLightClassForFacade
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.JavaRoot
import org.jetbrains.kotlin.cli.jvm.compiler.JvmCliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.ifEmpty
import java.io.File
import java.net.URL
import java.net.URLClassLoader

val KOTLIN_COMPILER_PATH = ProjectUtils.buildLibPath("kotlin-compiler")

fun getEnvironment(eclipseFile: IFile): KotlinCommonEnvironment {
    return KotlinEnvironment.getEnvironment(eclipseFile.project)
}

fun getEnvironment(ideaProject: Project): KotlinCommonEnvironment? {
    val eclipseResource = getEclipseResource(ideaProject) ?: return null
    return KotlinEnvironment.getEnvironment(eclipseResource)
}

fun getEclipseResource(ideaProject: Project): IProject? {
    return KotlinEnvironment.getJavaProject(ideaProject)
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
        
        KotlinScriptDefinitionProvider.getInstance(project).addScriptDefinition(StandardScriptDefinition)
        
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
            KotlinPsiManager.invalidateCachedProjectSourceFiles()
        }
        
        @JvmStatic fun removeEnvironment(eclipseProject: IProject) {
            cachedEnvironment.removeEnvironment(eclipseProject)
        }

        @JvmStatic fun getJavaProject(project: Project): IProject? = cachedEnvironment.getEclipseResource(project)
    }
}