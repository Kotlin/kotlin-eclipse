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
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.lang.descriptors.DependencyKind;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.kotlin.core.injectors.EclipseInjectorForTopDownAnalyzerForJvm;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;

public enum EclipseAnalyzerFacadeForJVM implements AnalyzerFacade {

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
    public static AnalyzeExhaust analyzeFilesWithJavaIntegration(
            IJavaProject javaProject, 
            Project project,
            Collection<JetFile> files,
            BindingTrace trace,
            Predicate<PsiFile> filesToAnalyzeCompletely,
            ModuleDescriptorImpl module
    ) {
        GlobalContext globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(),
                globalContext.getExceptionTracker(),
                filesToAnalyzeCompletely,
                false,
                false
        );

        EclipseInjectorForTopDownAnalyzerForJvm injector = new EclipseInjectorForTopDownAnalyzerForJvm(project, 
                topDownAnalysisParameters, trace, module, javaProject);
        try {
            module.addFragmentProvider(DependencyKind.BINARIES, injector.getJavaDescriptorResolver().getPackageFragmentProvider());
            injector.getTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, files);
            return AnalyzeExhaust.success(trace.getBindingContext(), module);
        }
        finally {
            injector.destroy();
        }
    }

    @NotNull
    public static ModuleDescriptorImpl createJavaModule(@NotNull String name) {
        return new ModuleDescriptorImpl(Name.special(name), DEFAULT_IMPORTS, JavaToKotlinClassMap.getInstance());
    }

    @Override
    @NotNull
    public Setup createSetup(@NotNull Project project, @NotNull Collection<JetFile> syntheticFiles,
            @NotNull GlobalSearchScope filesScope) {
        throw new UnsupportedOperationException();
    }
}
