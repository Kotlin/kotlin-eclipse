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

import java.util.LinkedHashSet
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.frontend.java.di.ContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.util.HashSet
import org.jetbrains.kotlin.utils.KotlinFrontEndException
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.config.LanguageVersion

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
                KotlinPackagePartProvider(environment.javaProject),
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
    
    private fun getPath(jetFile: KtFile): String? = jetFile.getVirtualFile()?.getPath()
}