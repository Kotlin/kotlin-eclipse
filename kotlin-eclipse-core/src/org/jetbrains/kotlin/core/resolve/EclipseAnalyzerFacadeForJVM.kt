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
import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.frontend.java.di.ContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.util.HashSet
import org.jetbrains.kotlin.incremental.components.UsageCollector

public object EclipseAnalyzerFacadeForJVM {
    public fun analyzeFilesWithJavaIntegration(javaProject: IJavaProject, project: Project, filesToAnalyze: Collection<JetFile>): AnalysisResult {
        val allFiles = LinkedHashSet<JetFile>(filesToAnalyze)
        val addedFiles = filesToAnalyze.map { getPath(it) }
        
        ProjectUtils.getSourceFilesWithDependencies(javaProject).filterNotTo(allFiles) {
            getPath(it) in addedFiles
        }
        
        val globalContext = GlobalContext()
        val providerFactory = FileBasedDeclarationProviderFactory(globalContext.storageManager, allFiles)
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project)
        val trace = CliLightClassGenerationSupport.CliBindingTrace()
        
        val container = createContainerForTopDownAnalyzerForJvm(moduleContext, trace, providerFactory, 
                GlobalSearchScope.allScope(project), javaProject, UsageCollector.DO_NOTHING)
        val additionalProviders = listOf(container.javaDescriptorResolver.packageFragmentProvider)
        container.lazyTopDownAnalyzerForTopLevel.analyzeFiles(TopDownAnalysisMode.TopLevelDeclarations, filesToAnalyze, additionalProviders)
        
        return AnalysisResult.success(trace.getBindingContext(), moduleContext.module)
    }
    
    private fun getPath(jetFile: JetFile): String = jetFile.getVirtualFile().getPath()
}