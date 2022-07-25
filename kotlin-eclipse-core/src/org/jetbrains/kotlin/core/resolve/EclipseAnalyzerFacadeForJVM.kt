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
package org.jetbrains.kotlin.core.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.search.GlobalSearchScope
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.EclipseScriptDefinitionProvider
import org.jetbrains.kotlin.core.model.KotlinCommonEnvironment
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.jetbrains.kotlin.core.preferences.languageVersionSettings
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.core.utils.asResource
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformConfigurator
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.KotlinFrontEndException
import kotlin.math.absoluteValue
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.util.PropertiesCollection

data class AnalysisResultWithProvider(val analysisResult: AnalysisResult, val componentProvider: ComponentProvider?) {
    companion object {
        val EMPTY = AnalysisResultWithProvider(AnalysisResult.EMPTY, null)
    }
}

object EclipseAnalyzerFacadeForJVM {
    fun analyzeSources(
        environment: KotlinEnvironment,
        filesToAnalyze: Collection<KtFile>
    ): AnalysisResultWithProvider {
        val filesSet = filesToAnalyze.toSet()
        if (filesSet.size != filesToAnalyze.size) {
            KotlinLogger.logWarning("Analyzed files have duplicates")
        }

        val allFiles = LinkedHashSet<KtFile>(filesSet)
        val addedFiles = filesSet.mapNotNull { getPath(it) }.toSet()
        ProjectUtils.getSourceFilesWithDependencies(environment.javaProject).filterNotTo(allFiles) {
            getPath(it) in addedFiles
        }

        return analyzeKotlin(
            filesToAnalyze = filesSet,
            allFiles = allFiles,
            environment = environment,
            javaProject = environment.javaProject,
            jvmTarget = environment.compilerProperties.jvmTarget
        )
    }

    fun analyzeScript(
        environment: KotlinScriptEnvironment,
        scriptFile: KtFile
    ): AnalysisResultWithProvider {
        //TODO actually take dependencies from script configuration!
        val javaProject = environment.javaProject/*.apply {
            /*tempClasspath += JavaRuntime.getDefaultVMInstall()
                    ?.let { JavaRuntime.getLibraryLocations(it) }
                    ?.map { JavaCore.newLibraryEntry(it.systemLibraryPath, null, null) }
                    .orEmpty()*/

            /*tempClasspath += (environment.dependencies
                    ?.classpath.orEmpty() + environment.definitionClasspath)
                    .map { JavaCore.newLibraryEntry(Path(it.absolutePath), null, null) }*/

        try {
            val tempSourceCode = KtFileScriptSource(scriptFile)
            val tempContribution = EclipseScriptDefinitionProvider.getContribution(tempSourceCode)

            val tempNewClasspath = tempContribution?.createClasspath(environment) ?: (tempOrigClasspath + environment.definitionClasspath.map {
                JavaCore.newLibraryEntry(Path(it.absolutePath), null, null)
            }).distinctBy { it.path }.toTypedArray()

            javaProject.setRawClasspath(tempNewClasspath, null)

        val tempRefinedConfig = environment.definition?.let {
            refineScriptCompilationConfiguration(tempSourceCode, it, environment.project)
        }?.valueOrNull()?.configuration

        val tempContribution = EclipseScriptDefinitionProvider.getContribution(tempSourceCode)

            val tempRefinedConfig = environment.definition?.let {
                refineScriptCompilationConfiguration(tempSourceCode, it, environment.project)
            }?.valueOrNull()?.configuration


            override val platformConfigurator: PlatformConfigurator = JvmPlatformConfigurator

            override fun computePlatformSpecificDefaultImports(
                storageManager: StorageManager,
                result: MutableList<ImportPath>
            ) {
                result.add(ImportPath.fromString("kotlin.jvm.*"))
                tempImports.map(ImportPath::fromString).toCollection(result)

                fun addAllClassifiersFromScope(scope: MemberScope) {
                    for (descriptor in scope.getContributedDescriptors(
                        DescriptorKindFilter.CLASSIFIERS,
                        MemberScope.ALL_NAME_FILTER
                    )) {
                        result.add(ImportPath(DescriptorUtils.getFqNameSafe(descriptor), false))
                    }
                }

            val tempProperties =
                tempRefinedConfig?.get(PropertiesCollection.Key("providedProperties", emptyMap<String, KotlinType>()))

            val declarationProviderFactory: (StorageManager) -> DeclarationProviderFactory = { storageManager ->
                object : FileBasedDeclarationProviderFactory(storageManager, allFiles) {

                    private val factory = KtPsiFactory(environment.project, true)

                    override fun getClassMemberDeclarationProvider(classLikeInfo: KtClassLikeInfo): ClassMemberDeclarationProvider {
                        if (classLikeInfo is KtScriptInfo) {
                            return object : AbstractPsiBasedDeclarationProvider(storageManager),
                                ClassMemberDeclarationProvider {
                                override val ownerInfo: KtClassLikeInfo = classLikeInfo

                                override fun doCreateIndex(index: Index) {
                                    val tempProvidedProperties = tempProperties?.entries?.map { (key, value) ->
                                        val isNullable = tempContribution?.isNullable(key, tempRefinedConfig) ?: true
                                        val tempTypeName = value.fromClass?.qualifiedName ?: value.typeName
                                        val tempText =
                                            """
                                                /** Provided property '$key' of type: $tempTypeName */
                                                val $key: $tempTypeName${'$'}${if (isNullable) "? = null" else " = TODO()"}""".trimIndent()
                                        factory.createProperty(tempText)
                                    } ?: emptyList()

                                    val tempDeclarations = ownerInfo.declarations +
                                            ownerInfo.primaryConstructorParameters +
                                            tempProvidedProperties

                                    tempDeclarations.forEach(index::putToIndex)
                                }
                            }
                        }
                        return super.getClassMemberDeclarationProvider(classLikeInfo)
                    }
                }
            }
        }

        val tempProperties =
            tempRefinedConfig?.get(PropertiesCollection.Key("providedProperties", emptyMap<String, KotlinType>()))

        if (!tempProperties.isNullOrEmpty()) {
            val tempPackageName = "scriptParameters${scriptFile.virtualFilePath.hashCode().absoluteValue}"
            val tempContent =
                "package $tempPackageName\n" + tempProperties.entries.joinToString(separator = "\n") { (key, value) ->
                    val isNullable = tempContribution?.isNullable(key, tempRefinedConfig) ?: true
                    """
                        |@Deprecated(message = "Do not import this explicitly! Used only in eclipse as workaround for providedProperties in Scripts!", level = DeprecationLevel.WARNING)
                        |val $key: ${value.typeName}${if(isNullable) "? = null" else " = TODO()"}
                    """.trimMargin("|")
                }

            tempImports.add("$tempPackageName.*")

            val tempKtFile = PsiFileFactory.getInstance(environment.project)
                .createFileFromText("scriptParameters.kt", KotlinLanguage.INSTANCE, tempContent) as? KtFile

            if (tempKtFile != null) {
                allFiles.add(tempKtFile)
            }
        }

        return analyzeKotlin(
            filesToAnalyze = listOf(scriptFile),
            allFiles = allFiles,
            environment = environment,
            javaProject = javaProject,
            analyzerService = analyzerService
        )
    }

    private fun analyzeKotlin(
        filesToAnalyze: Collection<KtFile>,
        allFiles: Collection<KtFile>,
        environment: KotlinCommonEnvironment,
        javaProject: IJavaProject?,
        jvmTarget: JvmTarget = JvmTarget.DEFAULT,
        analyzerService: PlatformDependentAnalyzerServices? = null
    ): AnalysisResultWithProvider {
        val project = environment.project
        val moduleContext = createModuleContext(project, environment.configuration, true)
        val storageManager = moduleContext.storageManager
        val module = moduleContext.module

        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, allFiles)
        val trace = CliBindingTrace()

        val sourceScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, filesToAnalyze)
        val moduleClassResolver = TopDownAnalyzerFacadeForJVM.SourceOrBinaryModuleClassResolver(sourceScope)

        val languageVersionSettings =
            javaProject?.project?.let { KotlinEnvironment.getEnvironment(it).compilerProperties.languageVersionSettings }
                ?: LanguageVersionSettingsImpl(
                    LanguageVersionSettingsImpl.DEFAULT.languageVersion,
                    LanguageVersionSettingsImpl.DEFAULT.apiVersion
                )


        val optionalBuiltInsModule = JvmBuiltIns(storageManager, JvmBuiltIns.Kind.FROM_CLASS_LOADER)
            .apply { initialize(module, true) }
            .builtInsModule

        val dependencyModule = run {
            val dependenciesContext = ContextForNewModule(
                moduleContext, Name.special(
                    "<dependencies of ${
                        environment.configuration.getNotNull(
                            CommonConfigurationKeys.MODULE_NAME
                        )
                    }>"
                ),
                module.builtIns, null
            )

            val dependencyScope = GlobalSearchScope.notScope(sourceScope)
            val dependenciesContainer = createContainerForTopDownAnalyzerForJvm(
                dependenciesContext,
                trace,
                DeclarationProviderFactory.EMPTY,
                dependencyScope,
                LookupTracker.DO_NOTHING,
                KotlinPackagePartProvider(environment),
                jvmTarget,
                languageVersionSettings,
                moduleClassResolver,
                javaProject,
                environment.project.getService(JavaModuleResolver::class.java),
                null
            )

            moduleClassResolver.compiledCodeResolver = dependenciesContainer.get<JavaDescriptorResolver>()

            dependenciesContext.setDependencies(listOfNotNull(dependenciesContext.module, optionalBuiltInsModule))
            dependenciesContext.initializeModuleContents(
                CompositePackageFragmentProvider(
                    listOf(
                        moduleClassResolver.compiledCodeResolver.packageFragmentProvider,
                        dependenciesContainer.get<JvmBuiltInsPackageFragmentProvider>()
                    ), ""
                )
            )
            dependenciesContext.module
        }

        val container = createContainerForTopDownAnalyzerForJvm(
            moduleContext,
            trace,
            providerFactory,
            sourceScope,
            LookupTracker.DO_NOTHING,
            KotlinPackagePartProvider(environment),
            jvmTarget,
            languageVersionSettings,
            moduleClassResolver,
            javaProject,
            environment.project.getService(JavaModuleResolver::class.java),
            analyzerService
        ).apply {
            initJvmBuiltInsForTopDownAnalysis()
        }

        moduleClassResolver.sourceCodeResolver = container.get<JavaDescriptorResolver>()

        val additionalProviders = ArrayList<PackageFragmentProvider>()
        additionalProviders.add(container.get<JavaDescriptorResolver>().packageFragmentProvider)

        PackageFragmentProviderExtension.getInstances(project).mapNotNullTo(additionalProviders) { extension ->
            extension.getPackageFragmentProvider(project, module, storageManager, trace, null, LookupTracker.DO_NOTHING)
        }

        module.setDependencies(
            listOfNotNull(module, dependencyModule, optionalBuiltInsModule),
            setOf(dependencyModule)
        )
        module.initialize(
            CompositePackageFragmentProvider(
                listOf(container.get<KotlinCodeAnalyzer>().packageFragmentProvider) +
                        additionalProviders, ""
            )
        )

        try {
            container.get<LazyTopDownAnalyzer>()
                .analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, filesToAnalyze)
        } catch (e: KotlinFrontEndException) {
//          Editor will break if we do not catch this exception
//          and will not be able to save content without reopening it.
//          In IDEA this exception throws only in CLI
            KotlinLogger.logError(e)
        }

        return AnalysisResultWithProvider(
            AnalysisResult.success(trace.bindingContext, module),
            container
        )
    }

    private fun getPath(jetFile: KtFile): String? = jetFile.virtualFile?.path

    private fun createModuleContext(
        project: Project,
        configuration: CompilerConfiguration,
        createBuiltInsFromModule: Boolean
    ): MutableModuleContext {
        val projectContext = ProjectContext(project, "context for project ${project.name}")
        val builtIns = JvmBuiltIns(
            projectContext.storageManager,
            if (createBuiltInsFromModule) JvmBuiltIns.Kind.FROM_DEPENDENCIES else JvmBuiltIns.Kind.FROM_CLASS_LOADER
        )
        return ContextForNewModule(
            projectContext,
            Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>"),
            builtIns,
            null
        ).apply {
            if (createBuiltInsFromModule) {
                builtIns.builtInsModule = module
            }
        }
    }
}
