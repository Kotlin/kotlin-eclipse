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
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.jdt.core.IClasspathContainer
import org.eclipse.jdt.core.IClasspathEntry
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.osgi.internal.loader.EquinoxClassLoader
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.cli.common.script.CliScriptDefinitionProvider
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCliJavaFileManagerImpl
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
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
import org.jetbrains.kotlin.core.buildLibPath
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.filesystem.KotlinLightClassManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.preferences.CompilerPlugin
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.core.resolve.lang.kotlin.EclipseVirtualFileFinderFactory
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.AnnotationBasedExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.load.java.sam.SamWithReceiverResolver
import org.jetbrains.kotlin.load.kotlin.MetadataFinderFactory
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptDefinitionProvider
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.utils.ifEmpty
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.*
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
        val eclipseFile: IFile,
        val loadScriptDefinitions: Boolean,
        val scriptDefinitions: List<KotlinScriptDefinition>,
        val providersClasspath: List<String>,
        var externalDependencies: ScriptDependencies? = null,
        disposalbe: Disposable) :
        KotlinCommonEnvironment(disposalbe) {
    init {
        val scriptsForProvider = scriptDefinitions
                .filter { it.isScript(eclipseFile.name) }
                .ifEmpty { listOf(StandardScriptDefinition) }

        val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) as? CliScriptDefinitionProvider
        if (scriptDefinitionProvider != null) {
            scriptDefinitionProvider.setScriptDefinitions(scriptsForProvider)
        }


        addToCPFromScriptTemplateClassLoader(providersClasspath)

        configureClasspath()

        val ioFile = eclipseFile.location.toFile()
        val definition = ScriptDefinitionProvider.getInstance(project)?.findScriptDefinition(ioFile.name)
        if (!loadScriptDefinitions) {
            addToCPFromExternalDependencies(ScriptDependenciesProvider.getInstance(project))
        }

        val annotations = definition?.annotationsForSamWithReceivers
        if (annotations != null) {
            StorageComponentContainerContributor.registerExtension(project, CliSamWithReceiverComponentContributor(annotations))
        }

        val index = JvmDependenciesIndexImpl(getRoots().toList())

        val area = Extensions.getArea(project)
        with(area.getExtensionPoint(PsiElementFinder.EP_NAME)) {
            registerExtension(PsiElementFinderImpl(project, ServiceManager.getService(project, JavaFileManager::class.java)))
            registerExtension(KotlinScriptDependenciesClassFinder(project, eclipseFile))
        }

        index.indexedRoots.forEach {
            projectEnvironment.addSourcesToClasspath(it.file)
        }

        val (_, singleJavaFileRoots) =
                getRoots().partition { (file) -> file.isDirectory || file.extension != "java" }

        val fileManager = ServiceManager.getService(project, CoreJavaFileManager::class.java)
        (fileManager as KotlinCliJavaFileManagerImpl).initialize(
                index,
                emptyList(),
                SingleJavaFileRootsIndex(singleJavaFileRoots),
                configuration.getBoolean(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING))

        val finderFactory = CliVirtualFileFinderFactory(index)
        project.registerService(MetadataFinderFactory::class.java, finderFactory)
        project.registerService(VirtualFileFinderFactory::class.java, finderFactory)
    }

    companion object {
        private val KOTLIN_RUNTIME_PATH = KotlinClasspathContainer.LIB_RUNTIME_NAME.buildLibPath()
        private val KOTLIN_SCRIPT_RUNTIME_PATH = KotlinClasspathContainer.LIB_SCRIPT_RUNTIME_NAME.buildLibPath()

        private val cachedEnvironment = CachedEnvironment<IFile, KotlinScriptEnvironment>()

        @JvmStatic
        fun getEnvironment(file: IFile): KotlinScriptEnvironment {
            checkIsScript(file)

            return cachedEnvironment.getOrCreateEnvironment(file) {
                KotlinScriptEnvironment(it, true, listOf(), listOf(), null, Disposer.newDisposable())
            }
        }

        @JvmStatic
        fun removeKotlinEnvironment(file: IFile) {
            checkIsScript(file)

            cachedEnvironment.removeEnvironment(file)
        }

        fun replaceEnvironment(
                file: IFile,
                scriptDefinitions: List<KotlinScriptDefinition>,
                providersClasspath: List<String>,
                previousExternalDependencies: ScriptDependencies?): KotlinScriptEnvironment {
            checkIsScript(file)
            val environment = cachedEnvironment.replaceEnvironment(file) {
                KotlinScriptEnvironment(file, false, scriptDefinitions, providersClasspath, previousExternalDependencies, Disposer.newDisposable())
            }
            KotlinPsiManager.removeFile(file)

            return environment
        }

        @JvmStatic
        fun getEclipseFile(project: Project): IFile? = cachedEnvironment.getEclipseResource(project)

        fun isScript(file: IFile): Boolean {
            return file.fileExtension == KotlinParserDefinition.STD_SCRIPT_SUFFIX // TODO: use ScriptDefinitionProvider
        }

        @JvmStatic
        fun constructFamilyForInitialization(file: IFile): String = file.fullPath.toPortableString() + "scriptDef"

        private fun checkIsScript(file: IFile) {
            if (!isScript(file)) {
                throw IllegalArgumentException("KotlinScriptEnvironment can work only with scripts, not ${file.name}")
            }
        }
    }

    @Volatile
    var isScriptDefinitionsInitialized = !loadScriptDefinitions
        private set

    @Volatile
    var isInitializingScriptDefinitions = false

    @Synchronized
    fun initializeScriptDefinitions(postTask: (List<KotlinScriptDefinition>, List<String>) -> Unit) {
        if (isScriptDefinitionsInitialized || isInitializingScriptDefinitions) return
        isInitializingScriptDefinitions = true

        val definitions = arrayListOf<KotlinScriptDefinition>()
        val classpath = arrayListOf<String>()
        runJob("Initialize Script Definitions", Job.DECORATE, constructFamilyForInitialization(eclipseFile), { monitor ->
            val definitionsAndClasspath = loadAndCreateDefinitionsByTemplateProviders(eclipseFile, monitor)
            KotlinLogger.logInfo("Found definitions: ${definitionsAndClasspath.first.joinToString()}")
            definitions.addAll(definitionsAndClasspath.first)
            classpath.addAll(definitionsAndClasspath.second)

            monitor.done()

            Status.OK_STATUS
        }, { _ ->
            isScriptDefinitionsInitialized = true
            isInitializingScriptDefinitions = false
            postTask(definitions, classpath)
        })
    }

    private fun configureClasspath() {
        addToClasspath(KOTLIN_RUNTIME_PATH.toFile())
        addToClasspath(KOTLIN_SCRIPT_RUNTIME_PATH.toFile())
        addJREToClasspath()
    }

    private fun addToCPFromScriptTemplateClassLoader(cp: List<String>) {
        for (entry in cp) {
            addToClasspath(File(entry), JavaRoot.RootType.BINARY)
        }
    }

    private fun addToCPFromExternalDependencies(dependenciesProvider: ScriptDependenciesProvider?) {
        if (dependenciesProvider == null) return

        val dependencies = if (externalDependencies != null)
            externalDependencies
        else {
            val scriptDependencies = dependenciesProvider.getScriptDependencies(KotlinPsiManager.getParsedFile(eclipseFile))
            externalDependencies = scriptDependencies
            scriptDependencies
        }
        if (dependencies != null) {
            for (dep in dependencies.classpath) {
                addToClasspath(dep)
            }
        }
    }

    private fun addToCPFromScriptTemplateClassLoader() {
        val ioFile = eclipseFile.getLocation().toFile()
        val definition = ScriptDefinitionProvider.getInstance(project)?.findScriptDefinition(ioFile.name)

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


private fun classpathFromClassloader(classLoader: ClassLoader): List<File> {
    return when (classLoader) {
        is URLClassLoader -> {
            classLoader.urLs
                    ?.mapNotNull { it.toFile() }
                    ?: emptyList()
        }
        is EquinoxClassLoader -> {
            classLoader.classpathManager.hostClasspathEntries.map { entry ->
                entry.bundleFile.baseFile
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
        if (platform != JvmPlatform) return

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

    private val _compilerProperties: KotlinProperties = KotlinProperties(ProjectScope(eclipseProject))

    val compilerProperties: KotlinProperties
        get() = _compilerProperties.takeIf { it.globalsOverridden } ?: KotlinProperties.workspaceInstance

    val index by lazy { JvmDependenciesIndexImpl(getRoots().toList()) }

    init {
        registerProjectDependenServices(javaProject)
        configureClasspath(javaProject)

        with(project) {
            registerService(KtLightClassForFacade.FacadeStubCache::class.java, KtLightClassForFacade.FacadeStubCache(project))
        }

        val scriptDefinitionProvider = ScriptDefinitionProvider.getInstance(project) as? CliScriptDefinitionProvider
        if (scriptDefinitionProvider != null) {
            scriptDefinitionProvider.setScriptDefinitions(listOf(StandardScriptDefinition))
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
                ?.replace("\$KOTLIN_HOME", ProjectUtils.KT_HOME)
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
        fun getEnvironment(eclipseProject: IProject): KotlinEnvironment {
            return cachedEnvironment.getOrCreateEnvironment(eclipseProject, environmentCreation)
        }

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
