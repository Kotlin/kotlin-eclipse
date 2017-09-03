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
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDependenciesImpl
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.util.KotlinFrontEndException
import java.util.ArrayList
import java.util.LinkedHashSet
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm as createContainerForScript
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM.SourceOrBinaryModuleClassResolver
import org.jetbrains.kotlin.config.JvmTarget

public data class AnalysisResultWithProvider(val analysisResult: AnalysisResult, val componentProvider: ComponentProvider)

public object EclipseAnalyzerFacadeForJVM {
    public fun analyzeFilesWithJavaIntegration(
            environment: KotlinEnvironment,
            filesToAnalyze: Collection<KtFile>): AnalysisResultWithProvider {
        val filesSet = filesToAnalyze.toSet()
        if (filesSet.size != filesToAnalyze.size) {
            KotlinLogger.logWarning("Analyzed files have duplicates")
        }
        
        val allFiles = LinkedHashSet<KtFile>(filesSet)
        val addedFiles = filesSet.map { getPath(it) }.filterNotNull().toSet()
        ProjectUtils.getSourceFilesWithDependencies(environment.javaProject).filterNotTo(allFiles) {
            getPath(it) in addedFiles
        }
        
        val project = environment.project
        
        val moduleContext = createModuleContext(project, environment.configuration, true)
        val storageManager = moduleContext.storageManager
        val module = moduleContext.module
        
        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, allFiles)
        val trace = CliLightClassGenerationSupport.CliBindingTrace()
        
        val sourceScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, filesToAnalyze)
        val moduleClassResolver = SourceOrBinaryModuleClassResolver(sourceScope)

        val languageVersionSettings = LanguageVersionSettingsImpl(
                LanguageVersionSettingsImpl.DEFAULT.languageVersion,
                LanguageVersionSettingsImpl.DEFAULT.apiVersion)
        val optionalBuiltInsModule = JvmBuiltIns(storageManager).apply { initialize(module, true) }.builtInsModule
        
        val dependencyModule = run {
            val dependenciesContext = ContextForNewModule(
                    moduleContext, Name.special("<dependencies of ${environment.configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>"),
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
                JvmTarget.DEFAULT,
                languageVersionSettings,
                moduleClassResolver,
                environment.javaProject)
            
            moduleClassResolver.compiledCodeResolver = dependenciesContainer.get<JavaDescriptorResolver>()
            
            dependenciesContext.setDependencies(listOfNotNull(dependenciesContext.module, optionalBuiltInsModule))
            dependenciesContext.initializeModuleContents(CompositePackageFragmentProvider(listOf(
                    moduleClassResolver.compiledCodeResolver.packageFragmentProvider,
                    dependenciesContainer.get<JvmBuiltInsPackageFragmentProvider>()
            )))
            dependenciesContext.module
        }
        
        
        
        val container = createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                trace,
                providerFactory,
                sourceScope,
                LookupTracker.DO_NOTHING,
                KotlinPackagePartProvider(environment),
                JvmTarget.DEFAULT,
                languageVersionSettings,
                moduleClassResolver,
                environment.javaProject).apply {
            initJvmBuiltInsForTopDownAnalysis()
        }
        
        moduleClassResolver.sourceCodeResolver = container.get<JavaDescriptorResolver>()
        
        val additionalProviders = ArrayList<PackageFragmentProvider>()
        additionalProviders.add(container.get<JavaDescriptorResolver>().packageFragmentProvider)
        
        PackageFragmentProviderExtension.getInstances(project).mapNotNullTo(additionalProviders) { extension ->
            extension.getPackageFragmentProvider(project, module, storageManager, trace, null)
        }
        
        module.setDependencies(ModuleDependenciesImpl(
                listOfNotNull(module, dependencyModule, optionalBuiltInsModule),
                setOf(dependencyModule)
        ))
        module.initialize(CompositePackageFragmentProvider(
                listOf(container.get<KotlinCodeAnalyzer>().packageFragmentProvider) +
                additionalProviders
        ))
        
        try {
            container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, filesSet)
        } catch(e: KotlinFrontEndException) {
//          Editor will break if we do not catch this exception
//          and will not be able to save content without reopening it.
//          In IDEA this exception throws only in CLI
            KotlinLogger.logError(e)
        }
        
        return AnalysisResultWithProvider(
                AnalysisResult.success(trace.getBindingContext(), module),
                container)
    }
    
    public fun analyzeScript(
            environment: KotlinScriptEnvironment,
            scriptFile: KtFile): AnalysisResultWithProvider {
        
        val trace = CliLightClassGenerationSupport.CliBindingTrace()
        
        val container = TopDownAnalyzerFacadeForJVM.createContainer(
                environment.project,
                setOf(scriptFile),
                trace,
                environment.configuration,
                { KotlinPackagePartProvider(environment) },
                { storageManager: StorageManager, files: Collection<KtFile> -> FileBasedDeclarationProviderFactory(storageManager, files) }
        )
        
        try {
            container.get<LazyTopDownAnalyzer>().analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, setOf(scriptFile))
        } catch(e: KotlinFrontEndException) {
//          Editor will break if we do not catch this exception
//          and will not be able to save content without reopening it.
//          In IDEA this exception throws only in CLI
            KotlinLogger.logError(e)
        }
        
        return AnalysisResultWithProvider(
                AnalysisResult.success(trace.getBindingContext(), container.get<ModuleDescriptor>()),
                container)
    }
    
    private fun getPath(jetFile: KtFile): String? = jetFile.getVirtualFile()?.getPath()
    
    private fun createModuleContext(
            project: Project,
            configuration: CompilerConfiguration,
            createBuiltInsFromModule: Boolean
    ): MutableModuleContext {
        val projectContext = ProjectContext(project)
        val builtIns = JvmBuiltIns(projectContext.storageManager, !createBuiltInsFromModule)
        return ContextForNewModule(
                projectContext, Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>"), builtIns, null
        ).apply {
            if (createBuiltInsFromModule) {
                builtIns.builtInsModule = module
            }
        }
    }
}