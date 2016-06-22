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

import com.intellij.psi.search.GlobalSearchScope
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinCommonEnvironment
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.utils.KotlinFrontEndException
import java.util.LinkedHashSet
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm as createContainerForScript

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
        
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.configuration)
        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, allFiles)
        val trace = CliLightClassGenerationSupport.CliBindingTrace()
        
        val containerAndProvider = createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                trace,
                providerFactory, 
                GlobalSearchScope.allScope(environment.project),
                environment.javaProject,
                LookupTracker.DO_NOTHING,
                KotlinPackagePartProvider(environment),
                LanguageVersion.LATEST)
        val container = containerAndProvider.first
        val additionalProviders = listOf(container.javaDescriptorResolver.packageFragmentProvider)
        
        try {
            container.lazyTopDownAnalyzerForTopLevel.analyzeFiles(TopDownAnalysisMode.TopLevelDeclarations, filesSet, additionalProviders)
        } catch(e: KotlinFrontEndException) {
//          Editor will break if we do not catch this exception
//          and will not be able to save content without reopening it.
//          In IDEA this exception throws only in CLI
            KotlinLogger.logError(e)
        }
        
        return AnalysisResultWithProvider(
                AnalysisResult.success(trace.getBindingContext(), moduleContext.module),
                containerAndProvider.second)
    }
    
    public fun analyzeScript(
            environment: KotlinScriptEnvironment,
            scriptFile: KtFile): AnalysisResultWithProvider {
        
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.configuration)
        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, listOf(scriptFile))
        val trace = CliLightClassGenerationSupport.CliBindingTrace()
        
        val containerAndProvider = createContainerForTopDownAnalyzerForScript(
                moduleContext,
                trace,
                providerFactory, 
                GlobalSearchScope.allScope(environment.project),
                LookupTracker.DO_NOTHING,
                KotlinPackagePartProvider(environment),
                LanguageVersion.LATEST)
        val container = containerAndProvider.first
        val additionalProviders = listOf(container.javaDescriptorResolver.packageFragmentProvider)
        
        try {
            container.lazyTopDownAnalyzerForTopLevel.analyzeFiles(
                    TopDownAnalysisMode.TopLevelDeclarations, listOf(scriptFile), additionalProviders)
        } catch(e: KotlinFrontEndException) {
//          Editor will break if we do not catch this exception
//          and will not be able to save content without reopening it.
//          In IDEA this exception throws only in CLI
            KotlinLogger.logError(e)
        }
        
        return AnalysisResultWithProvider(
                AnalysisResult.success(trace.getBindingContext(), moduleContext.module),
                containerAndProvider.second)
    }
    
    private fun getPath(jetFile: KtFile): String? = jetFile.getVirtualFile()?.getPath()
}