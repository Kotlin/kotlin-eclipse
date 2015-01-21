/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

package org.jetbrains.kotlin.core.resolve;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.context.ContextPackage;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.core.injectors.EclipseInjectorForTopDownAnalyzerForJvm;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.TopDownAnalysisParameters;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

public enum EclipseAnalyzerFacadeForJVM {

    INSTANCE;

    public static final List<ImportPath> DEFAULT_IMPORTS = ImmutableList.of(
            new ImportPath("java.lang.*"),
            new ImportPath("kotlin.*"),
            new ImportPath("kotlin.jvm.*"),
            new ImportPath("kotlin.io.*")
    );

    private EclipseAnalyzerFacadeForJVM() {
    }

    @NotNull
    public static AnalysisResult analyzeFilesWithJavaIntegration(
            IJavaProject javaProject, 
            Project project,
            @NotNull final Collection<JetFile> filesToAnalyze, 
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            ModuleDescriptorImpl module
    ) {
        GlobalContext globalContext = ContextPackage.GlobalContext();
        
        LinkedHashSet<JetFile> allFiles = new LinkedHashSet<JetFile>();
        allFiles.addAll(ProjectUtils.getSourceFilesWithDependencies(javaProject));
        allFiles.addAll(filesToAnalyze);
        
        FileBasedDeclarationProviderFactory providerFactory = new FileBasedDeclarationProviderFactory(
                globalContext.getStorageManager(), allFiles);

        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(),
                globalContext.getExceptionTracker(),
                filesToAnalyzeCompletely,
                false,
                false
        );

        BindingTrace trace = new CliLightClassGenerationSupport.CliBindingTrace();
        
        EclipseInjectorForTopDownAnalyzerForJvm injector = new EclipseInjectorForTopDownAnalyzerForJvm(
               project, javaProject, globalContext, trace, module, providerFactory);
        try {
            List<PackageFragmentProvider> additionalProviders = Lists.newArrayList();
            additionalProviders.add(injector.getJavaDescriptorResolver().getPackageFragmentProvider());
            
            injector.getLazyTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, filesToAnalyze, additionalProviders);
            return AnalysisResult.success(trace.getBindingContext(), module);
        }
        finally {
            injector.destroy();
        }
    }

    @NotNull
    public static ModuleDescriptorImpl createJavaModule(@NotNull String name) {
        return new ModuleDescriptorImpl(Name.special(name), DEFAULT_IMPORTS, JavaToKotlinClassMap.INSTANCE);
    }
}
