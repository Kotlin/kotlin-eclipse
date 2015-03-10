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

import java.util.ArrayList;
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
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.TopDownAnalysisParameters;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;

public enum EclipseAnalyzerFacadeForJVM {

    INSTANCE;

    public static final List<ImportPath> DEFAULT_IMPORTS = buildDefaultImports();

    private static List<ImportPath> buildDefaultImports() {
        List<ImportPath> list = new ArrayList<ImportPath>();
        list.add(new ImportPath("java.lang.*"));
        list.add(new ImportPath("kotlin.*"));
        list.add(new ImportPath("kotlin.jvm.*"));
        list.add(new ImportPath("kotlin.io.*"));
        // all classes from package "kotlin" mapped to java classes are imported explicitly so that they take priority over classes from java.lang
        for (ClassDescriptor descriptor : JavaToKotlinClassMap.INSTANCE.allKotlinClasses()) {
            FqName fqName = DescriptorUtils.getFqNameSafe(descriptor);
            if (fqName.parent().equals(new FqName("kotlin"))) {
                list.add(new ImportPath(fqName, false));
            }
        }
        return list;
    }

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
                false,
                false
        );

        BindingTrace trace = new CliLightClassGenerationSupport.CliBindingTrace();
        
        EclipseInjectorForTopDownAnalyzerForJvm injector = new EclipseInjectorForTopDownAnalyzerForJvm(
               project, javaProject, globalContext, trace, module, providerFactory, GlobalSearchScope.allScope(project));
        try {
            List<PackageFragmentProvider> additionalProviders = Lists.newArrayList();
            additionalProviders.add(injector.getJavaDescriptorResolver().getPackageFragmentProvider());
            
            injector.getLazyTopDownAnalyzerForTopLevel().analyzeFiles(topDownAnalysisParameters, filesToAnalyze, additionalProviders);
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
