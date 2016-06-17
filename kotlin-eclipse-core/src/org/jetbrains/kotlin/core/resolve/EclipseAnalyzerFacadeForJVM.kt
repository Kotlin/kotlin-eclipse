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

public data class AnalysisResultWithProvider(val analysisResult: AnalysisResult, val componentProvider: ComponentProvider?)

public object EclipseAnalyzerFacadeForJVM {
    public fun analyzeFilesWithJavaIntegration(javaProject: IJavaProject, project: Project, filesToAnalyze: Collection<KtFile>): AnalysisResultWithProvider {
        val filesSet = filesToAnalyze.toSet()
        if (filesSet.size != filesToAnalyze.size) {
            KotlinLogger.logWarning("Analyzed files have duplicates")
        }
        
        val allFiles = LinkedHashSet<KtFile>(filesSet)
        val addedFiles = filesSet.map { getPath(it) }.filterNotNull().toSet()
        ProjectUtils.getSourceFilesWithDependencies(javaProject).filterNotTo(allFiles) {
            getPath(it) in addedFiles
        }
        
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, project.getName())
        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, allFiles)
        val trace = CliLightClassGenerationSupport.CliBindingTrace()
        
<<<<<<< HEAD
        val containerAndProvider = createContainerForTopDownAnalyzerForJvm(moduleContext, trace, providerFactory, 
                GlobalSearchScope.allScope(project), javaProject, LookupTracker.DO_NOTHING, KotlinPackagePartProvider(javaProject))
=======
        val containerAndProvider = createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                trace,
                providerFactory, 
                GlobalSearchScope.allScope(environment.project),
                environment.javaProject,
                LookupTracker.DO_NOTHING,
                KotlinPackagePartProvider(environment),
                LanguageVersion.LATEST)
>>>>>>> ec8d6eb... Get rid of java project in package part provider
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
    
<<<<<<< HEAD
=======
    public fun analyzeScript(
            environment: KotlinScriptEnvironment,
            scriptFile: KtFile): AnalysisResultWithProvider {
        
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project, environment.configuration)
        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, listOf(scriptFile))
        val trace = CliLightClassGenerationSupport.CliBindingTrace()
        
        val componentProvider = createContainerForScript(
                moduleContext,
                trace,
                providerFactory, 
                GlobalSearchScope.allScope(environment.project),
                LookupTracker.DO_NOTHING,
                KotlinPackagePartProvider(environment),
                LanguageVersion.LATEST)
        val additionalProviders = listOf(componentProvider.javaDescriptorResolver.packageFragmentProvider)
        
        try {
            componentProvider.lazyTopDownAnalyzerForTopLevel.analyzeFiles(
                    TopDownAnalysisMode.TopLevelDeclarations, listOf(scriptFile), additionalProviders)
        } catch(e: KotlinFrontEndException) {
//          Editor will break if we do not catch this exception
//          and will not be able to save content without reopening it.
//          In IDEA this exception throws only in CLI
            KotlinLogger.logError(e)
        }
        
        return AnalysisResultWithProvider(AnalysisResult.success(trace.getBindingContext(), moduleContext.module), null)
    }
    
>>>>>>> 9cc72ba... Generalize api to analyze files with respect to scripts
    private fun getPath(jetFile: KtFile): String? = jetFile.getVirtualFile()?.getPath()
}