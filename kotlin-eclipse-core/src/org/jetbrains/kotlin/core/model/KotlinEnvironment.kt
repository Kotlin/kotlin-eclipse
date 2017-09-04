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
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.buildLibPath
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinder
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.ifEmpty
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.cli.common.script.CliScriptDependenciesProvider

val KOTLIN_COMPILER_PATH = ProjectUtils.buildLibPath("kotlin-compiler")

fun getEnvironment(eclipseFile: IFile): KotlinCommonEnvironment {
    return if (KotlinScriptEnvironment.isScript(eclipseFile)) {
        KotlinScriptEnvironment.getEnvironment(eclipseFile)
    } else {
        KotlinEnvironment.getEnvironment(eclipseFile.project)
    }
}

fun getEnvironment(ideaProject: Project): KotlinCommonEnvironment? {
    val eclipseResource = getEclipseResource(ideaProject) ?: return null
    return when (eclipseResource) {
        is IFile -> KotlinScriptEnvironment.getEnvironment(eclipseResource)
        is IProject -> KotlinEnvironment.getEnvironment(eclipseResource)
        else -> throw IllegalStateException("Could not get environment for resource: $eclipseResource")
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
        loadAndCreateDefinitionsByTemplateProviders(eclipseFile)
                .filter { it.isScript(eclipseFile.name) }
                .ifEmpty { listOf(StandardScriptDefinition) }
                .forEach {
                    KotlinScriptDefinitionProvider.getInstance(project)?.addScriptDefinition(it)
                }
        
        configureClasspath()
        
        project.registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(project))
        
        val index = JvmDependenciesIndexImpl(getRoots().toList())
        
        val (roots, singleJavaFileRoots) =
                getRoots().partition { (file) -> file.isDirectory || file.extension != "java" }
        
        project.registerService(VirtualFileFinderFactory::class.java, CliVirtualFileFinderFactory(index))
        
        val area = Extensions.getArea(project)
        with(area.getExtensionPoint(PsiElementFinder.EP_NAME)) {
            registerExtension(PsiElementFinderImpl(project, ServiceManager.getService(project, JavaFileManager::class.java)))
            registerExtension(KotlinScriptDependenciesClassFinder(project, eclipseFile))
        }
        
        val fileManager = ServiceManager.getService(project, CoreJavaFileManager::class.java)
        (fileManager as KotlinCliJavaFileManagerImpl).initialize(
                index,
                SingleJavaFileRootsIndex(singleJavaFileRoots),
                configuration.getBoolean(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING))
    }
    
    companion object {
        private val KOTLIN_RUNTIME_PATH = KotlinClasspathContainer.LIB_RUNTIME_NAME.buildLibPath()
        private val KOTLIN_SCRIPT_RUNTIME_PATH = KotlinClasspathContainer.LIB_SCRIPT_RUNTIME_NAME.buildLibPath()
        
        private val cachedEnvironment = CachedEnvironment<IFile, KotlinScriptEnvironment>()
        private val environmentCreation = {
            eclipseFile: IFile -> KotlinScriptEnvironment(eclipseFile, Disposer.newDisposable())
        }

        @JvmStatic fun getEnvironment(file: IFile): KotlinScriptEnvironment {
            checkIsScript(file)
            
            return cachedEnvironment.getOrCreateEnvironment(file, environmentCreation)
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
        addToClasspath(KOTLIN_RUNTIME_PATH.toFile())
        addToClasspath(KOTLIN_SCRIPT_RUNTIME_PATH.toFile())
        addJREToClasspath()
        addToCPFromScriptTemplateClassLoader()
    }
    
    private fun addToCPFromScriptTemplateClassLoader() {
        val ioFile = eclipseFile.getLocation().toFile()
        val definition = KotlinScriptDefinitionProvider.getInstance(project)?.findScriptDefinition(ioFile.name) ?: return

        if (definition is KotlinScriptDefinition) {
            val classLoader = definition.template.java.classLoader
            for (file in classpathFromClassloader(classLoader)) {
                addToClasspath(file, JavaRoot.RootType.BINARY)
            }
        }
    }
    
    private fun addJREToClasspath() {
        val project = eclipseFile.project
        if (JavaProject.hasJavaNature(project)) {
            val javaProject = JavaCore.create(project)
            javaProject.getRawClasspath().mapNotNull { entry ->
                if (entry.entryKind == IClasspathEntry.CPE_CONTAINER) {
                    val container = JavaCore.getClasspathContainer(entry.getPath(), javaProject)
                    if (container != null && container.kind == IClasspathContainer.K_DEFAULT_SYSTEM) {
                        return@mapNotNull container
                    }
                }
                
                null
            }
            .flatMap { it.getClasspathEntries().toList() }
            .flatMap { ProjectUtils.getFileByEntry(it, javaProject) }
            .forEach { addToClasspath(it) }
        }
    }
}

private fun URL.toFile() =
        try {
            File(toURI().schemeSpecificPart)
        } catch (e: java.net.URISyntaxException) {
            if (protocol != "file") null
            else File(file)
        }


private fun classpathFromClassloader(classLoader: ClassLoader): List<File> =
        (classLoader as? URLClassLoader)?.urLs
                ?.mapNotNull { it.toFile() }
                ?: emptyList()

class KotlinEnvironment private constructor(val eclipseProject: IProject, disposable: Disposable) :
        KotlinCommonEnvironment(disposable) {
    val javaProject = JavaCore.create(eclipseProject)
    
    val index by lazy { JvmDependenciesIndexImpl(getRoots().toList()) }
    
    init {
        registerProjectDependenServices(javaProject)
        configureClasspath(javaProject)
        
        with(project) {
            registerService(KtLightClassForFacade.FacadeStubCache::class.java, KtLightClassForFacade.FacadeStubCache(project))
        }
        
        KotlinScriptDefinitionProvider.getInstance(project)?.addScriptDefinition(StandardScriptDefinition)
        
        cachedEnvironment.putEnvironment(eclipseProject, this)
    }
    
    private fun registerProjectDependenServices(javaProject: IJavaProject) {
        project.registerService(VirtualFileFinderFactory::class.java, EclipseVirtualFileFinderFactory(javaProject))
        project.registerService(KotlinLightClassManager::class.java, KotlinLightClassManager(javaProject.project))
    }
    
    private fun configureClasspath(javaProject: IJavaProject) {
        if (!javaProject.exists()) return
        
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

        @JvmStatic fun removeEnvironment(eclipseProject: IProject) {
            cachedEnvironment.removeEnvironment(eclipseProject)
            KotlinPsiManager.invalidateCachedProjectSourceFiles()
            KotlinAnalysisFileCache.resetCache()
            KotlinAnalysisProjectCache.resetCache(eclipseProject)
        }

        @JvmStatic fun getJavaProject(project: Project): IProject? = cachedEnvironment.getEclipseResource(project)
    }
}