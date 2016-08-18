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
import org.jetbrains.kotlin.cli.jvm.compiler.JavaRoot
import org.jetbrains.kotlin.cli.jvm.compiler.JvmCliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.JvmDependenciesIndexImpl
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
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromTemplate
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.ifEmpty
import java.io.File
import java.net.URL
import java.net.URLClassLoader

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
                .filter { it.isScript(File(eclipseFile.name)) }
                .ifEmpty { listOf(StandardScriptDefinition) }
                .forEach {
                    KotlinScriptDefinitionProvider.getInstance(project).addScriptDefinition(it)
                }
        
        configureClasspath()
        
        project.registerService(KotlinJavaPsiFacade::class.java, KotlinJavaPsiFacade(project))
        
        val index = JvmDependenciesIndexImpl(getRoots().toList())
        
        project.registerService(JvmVirtualFileFinderFactory::class.java, JvmCliVirtualFileFinderFactory(index))
        
        val area = Extensions.getArea(project)
        area.getExtensionPoint(PsiElementFinder.EP_NAME).registerExtension(
                PsiElementFinderImpl(project, ServiceManager.getService(project, JavaFileManager::class.java)))
        
        val fileManager = ServiceManager.getService(project, CoreJavaFileManager::class.java)
        (fileManager as KotlinCliJavaFileManagerImpl).initIndex(index)
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
        addJREToClasspath()
        configureScriptDependencies()
    }
    
    private fun configureScriptDependencies() {
        val ioFile = eclipseFile.getLocation().toFile()
        val definition = KotlinScriptDefinitionProvider.getInstance(project).findScriptDefinition(ioFile) ?: return
        
        val dependencies = definition.getDependenciesFor(ioFile, project, null) ?: return
        for (entry in dependencies.classpath) {
            addToClasspath(entry)
        }
        
        if (definition is KotlinScriptDefinitionFromTemplate) {
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