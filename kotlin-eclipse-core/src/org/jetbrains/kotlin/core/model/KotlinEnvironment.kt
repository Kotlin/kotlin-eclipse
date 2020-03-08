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
import org.eclipse.core.resources.ProjectScope
import org.eclipse.jdt.core.IClasspathContainer
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.osgi.internal.loader.EquinoxClassLoader
import org.jetbrains.kotlin.asJava.classes.FacadeCache
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.index.JvmDependenciesIndexImpl
import org.jetbrains.kotlin.cli.jvm.index.SingleJavaFileRootsIndex
import org.jetbrains.kotlin.compiler.plugin.CliOptionValue
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.parsePluginOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.preferences.CompilerPlugin
import org.jetbrains.kotlin.core.preferences.KotlinBuildingProperties
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinderFactory
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.utils.asFile
import org.jetbrains.kotlin.core.utils.buildLibPath
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.load.java.sam.SamWithReceiverResolver
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.definitions.annotationsForSamWithReceivers
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.dependencies.ScriptDependencies

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

class KotlinScriptEnvironment private constructor(
        private val eclipseFile: IFile,
        val dependencies: ScriptDependencies?,
        disposable: Disposable
) : KotlinCommonEnvironment(disposable) {

    val definition: ScriptDefinition? = ScriptDefinitionProvider.getInstance(project)
            ?.findDefinition(KtFileScriptSource(KotlinPsiManager.getParsedFile(eclipseFile)))

    val definitionClasspath: Collection<File> = definition?.contextClassLoader?.let(::classpathFromClassloader).orEmpty()

    init {
        configureClasspath()

        configuration.put(ScriptingConfigurationKeys.SCRIPT_DEFINITIONS, listOfNotNull(definition))

        definition?.compilationConfiguration?.get(ScriptCompilationConfiguration.annotationsForSamWithReceivers)
            ?.map { it.typeName }
            ?.let { CliSamWithReceiverComponentContributor(it) }
            ?.also { StorageComponentContainerContributor.registerExtension(project, it) }

        val index = JvmDependenciesIndexImpl(getRoots().toList())

        val area = Extensions.getArea(project)
        with(area.getExtensionPoint(PsiElementFinder.EP_NAME)) {
            registerExtension(PsiElementFinderImpl(project, ServiceManager.getService(project, JavaFileManager::class.java)))
        }

        index.indexedRoots.forEach {
            projectEnvironment.addSourcesToClasspath(it.file)
        }

        val singleJavaFileRoots =
                getRoots().filter { !it.file.isDirectory && it.file.extension == "java" }

        val fileManager = ServiceManager.getService(project, CoreJavaFileManager::class.java)
        (fileManager as KotlinCliJavaFileManagerImpl).initialize(
                index,
                emptyList(),
                SingleJavaFileRootsIndex(singleJavaFileRoots),
                configuration.getBoolean(JVMConfigurationKeys.USE_PSI_CLASS_FILES_READING))

        val finderFactory = CliVirtualFileFinderFactory(index)
        project.registerService(MetadataFinderFactory::class.java, finderFactory)
        project.registerService(VirtualFileFinderFactory::class.java, finderFactory)

//        definition?.dependencyResolver?.also { project.registerService(DependenciesResolver::class.java, it) }
    }

    companion object {
        private val KOTLIN_RUNTIME_PATH = KotlinClasspathContainer.LIB_RUNTIME_NAME.buildLibPath()
        private val KOTLIN_SCRIPT_RUNTIME_PATH = KotlinClasspathContainer.LIB_SCRIPT_RUNTIME_NAME.buildLibPath()

        private val cachedEnvironment = CachedEnvironment<IFile, KotlinScriptEnvironment>()

        @JvmStatic
        fun getEnvironment(file: IFile): KotlinScriptEnvironment {
            checkIsScript(file)

            return cachedEnvironment.getOrCreateEnvironment(file) {
                KotlinScriptEnvironment(it, null, Disposer.newDisposable())
            }
        }

        @JvmStatic
        fun removeKotlinEnvironment(file: IFile) {
            checkIsScript(file)
            cachedEnvironment.removeEnvironment(file)
        }

        @JvmStatic
        fun getEclipseFile(project: Project): IFile? = cachedEnvironment.getEclipseResource(project)

        fun isScript(file: IFile): Boolean =
            EclipseScriptDefinitionProvider().isScript(KtFileScriptSource(KotlinPsiManager.getParsedFile(file)))

        private fun checkIsScript(file: IFile) {
            if (!isScript(file)) {
                throw IllegalArgumentException("KotlinScriptEnvironment can work only with scripts, not ${file.name}")
            }
        }

        fun updateDependencies(file: IFile, newDependencies: ScriptDependencies?) {
            cachedEnvironment.replaceEnvironment(file) {
                KotlinScriptEnvironment(file, newDependencies, Disposer.newDisposable())
                        .apply { addDependenciesToClasspath(newDependencies) }
            }
            KotlinPsiManager.removeFile(file)
        }
    }

    private fun configureClasspath() {
        addToClasspath(KOTLIN_RUNTIME_PATH.toFile())
        addToClasspath(KOTLIN_SCRIPT_RUNTIME_PATH.toFile())
        addJREToClasspath()

        definitionClasspath.forEach { addToClasspath(it) }
    }

    private fun addDependenciesToClasspath(dependencies: ScriptDependencies?) {
        dependencies?.classpath?.forEach {
            addToClasspath(it)
        }
    }

    private fun addJREToClasspath() {
        val project = eclipseFile.project
        if (JavaProject.hasJavaNature(project)) {
            val javaProject = JavaCore.create(project)
            javaProject.rawClasspath.mapNotNull { entry ->
                if (entry.entryKind == IClasspathEntry.CPE_CONTAINER) {
                    val container = JavaCore.getClasspathContainer(entry.path, javaProject)
                    if (container != null && container.kind == IClasspathContainer.K_DEFAULT_SYSTEM) {
                        return@mapNotNull container
                    }
                }

                null
            }
                    .flatMap { it.classpathEntries.toList() }
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


private fun classpathFromClassloader(classLoader: ClassLoader): List<File> {
    return when (classLoader) {
        is URLClassLoader -> {
            classLoader.urLs
                    ?.mapNotNull { it.toFile() }
                    ?: emptyList()
        }
        is EquinoxClassLoader -> {
            classLoader.classpathManager.hostClasspathEntries.map { entry ->
                entry.bundleFile.baseFile.resolve("bin")
            }
        }
        else -> {
            KotlinLogger.logWarning("Could not get dependencies from $classLoader for script provider")
            emptyList()
        }
    }
}

class CliSamWithReceiverComponentContributor(val annotations: List<String>) : StorageComponentContainerContributor {
    override fun registerModuleComponents(container: StorageComponentContainer, platform: TargetPlatform, moduleDescriptor: ModuleDescriptor) {
        if (platform.filterIsInstance<JvmPlatform>().isEmpty() ) return

        container.useInstance(SamWithReceiverResolverExtension(annotations))
    }
}

inline fun <reified T : Any> ComponentProvider.get(): T {
    return getService(T::class.java)
}

fun <T : Any> ComponentProvider.getService(request: Class<T>): T {
    return tryGetService(request) ?: throw IllegalArgumentException("Unresolved service: $request")
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> ComponentProvider.tryGetService(request: Class<T>): T? {
    return resolve(request)?.getValue() as T?
}

class SamWithReceiverResolverExtension(
        private val annotations: List<String>
) : SamWithReceiverResolver, AnnotationBasedExtension {
    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?) = annotations

    override fun shouldConvertFirstSamParameterToReceiver(function: FunctionDescriptor): Boolean {
        return (function.containingDeclaration as? ClassDescriptor)?.hasSpecialAnnotation(null) ?: false
    }
}

class KotlinEnvironment private constructor(val eclipseProject: IProject, disposable: Disposable) :
        KotlinCommonEnvironment(disposable) {

    val javaProject = JavaCore.create(eclipseProject)

    val projectCompilerProperties: KotlinProperties = KotlinProperties(ProjectScope(eclipseProject))

    val compilerProperties: KotlinProperties
        get() = projectCompilerProperties.takeIf { it.globalsOverridden } ?: KotlinProperties.workspaceInstance

    val projectBuildingProperties: KotlinBuildingProperties = KotlinBuildingProperties(ProjectScope(eclipseProject))

    val buildingProperties: KotlinBuildingProperties
        get() = projectBuildingProperties.takeIf { it.globalsOverridden } ?: KotlinBuildingProperties.workspaceInstance

    val index by lazy { JvmDependenciesIndexImpl(getRoots().toList()) }

    init {
        registerProjectDependenServices(javaProject)
        configureClasspath(javaProject)

        with(project) {
            registerService(FacadeCache::class.java, FacadeCache(project))
        }

        registerCompilerPlugins()

        cachedEnvironment.putEnvironment(eclipseProject, this)
    }

    private fun registerProjectDependenServices(javaProject: IJavaProject) {
        val finderFactory = EclipseVirtualFileFinderFactory(javaProject)
        project.registerService(VirtualFileFinderFactory::class.java, finderFactory)
        project.registerService(MetadataFinderFactory::class.java, finderFactory)
        project.registerService(KotlinLightClassManager::class.java, KotlinLightClassManager(javaProject.project))
    }

    private fun registerCompilerPlugins() {
        compilerProperties.compilerPlugins.entries
                .filter { it.active }
                .forEach { registerCompilerPlugin(it) }
    }

    private fun registerCompilerPlugin(it: CompilerPlugin) {
        val jarLoader = it.jarPath
                ?.replace("\$KOTLIN_HOME", ProjectUtils.ktHome)
                ?.let { URL("file://$it") }
                ?.let { URLClassLoader(arrayOf(it), this::class.java.classLoader) }

        val cliProcessor = jarLoader?.loadService<CommandLineProcessor>()
        val registrar = jarLoader?.loadService<ComponentRegistrar>()

        if (cliProcessor != null && registrar != null) {
            with(cliProcessor) {
                val configuration = CompilerConfiguration().apply { applyOptionsFrom(parseOptions(it.args), pluginOptions) }
                registrar.registerProjectComponents(project, configuration)
            }
        }
    }

    private inline fun <reified T : Any> ClassLoader.loadService(): T? =
            ServiceLoader.load(T::class.java, this)
                    .singleOrNull { it::class.java.classLoader == this }

    private fun parseOptions(args: List<String>): Map<String, List<String>> =
            args.asSequence()
                    .map { parsePluginOption("plugin:$it") }
                    .filterNotNull()
                    .groupBy(CliOptionValue::optionName, CliOptionValue::value)

    private fun configureClasspath(javaProject: IJavaProject) {
        if (!javaProject.exists()) return

        for (file in ProjectUtils.collectClasspathWithDependenciesForBuild(javaProject)) {
            addToClasspath(file)
        }
    }

    companion object {
        private val cachedEnvironment = CachedEnvironment<IProject, KotlinEnvironment>()
        private val environmentCreation = { eclipseProject: IProject ->
            KotlinEnvironment(eclipseProject, Disposer.newDisposable())
        }

        @JvmStatic
        fun getEnvironment(eclipseProject: IProject): KotlinEnvironment =
            cachedEnvironment.getOrCreateEnvironment(eclipseProject, environmentCreation)

        @JvmStatic
        fun removeEnvironment(eclipseProject: IProject) {
            cachedEnvironment.removeEnvironment(eclipseProject)
            KotlinPsiManager.invalidateCachedProjectSourceFiles()
            KotlinAnalysisFileCache.resetCache()
            KotlinAnalysisProjectCache.resetCache(eclipseProject)
        }

        @JvmStatic
        fun removeAllEnvironments() {
            cachedEnvironment.removeAllEnvironments()
            KotlinPsiManager.invalidateCachedProjectSourceFiles()
            KotlinAnalysisFileCache.resetCache()
            KotlinAnalysisProjectCache.resetAllCaches()
        }

        @JvmStatic
        fun getJavaProject(project: Project): IProject? = cachedEnvironment.getEclipseResource(project)

    }
}
