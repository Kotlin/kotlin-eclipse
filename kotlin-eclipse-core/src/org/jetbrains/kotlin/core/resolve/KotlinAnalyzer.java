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
package org.jetbrains.kotlin.core.resolve;

import java.util.Collection;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.psi.JetFile;

import com.google.common.collect.Lists;

public class KotlinAnalyzer {

    @NotNull
    public static AnalysisResult analyzeProject(@NotNull IJavaProject javaProject) {
        return analyzeProject(javaProject, ProjectUtils.getSourceFiles(javaProject.getProject()));
    }
    
    @NotNull
    public static AnalysisResult analyzeFile(@NotNull IJavaProject javaProject, @NotNull JetFile jetFile) {
        return analyzeProject(javaProject, Lists.newArrayList(jetFile));
    }
    
    private static AnalysisResult analyzeProject(@NotNull IJavaProject javaProject, 
            @NotNull Collection<JetFile> filesToAnalyze) {
        KotlinEnvironment kotlinEnvironment = KotlinEnvironment.getEnvironment(javaProject);
        return analysisResultProject(javaProject, kotlinEnvironment, filesToAnalyze);
    }
    
    @NotNull
    private static AnalysisResult analysisResultProject(@NotNull IJavaProject javaProject, @NotNull KotlinEnvironment kotlinEnvironment, 
            @NotNull Collection<JetFile> filesToAnalyze) {
        ModuleDescriptorImpl module = EclipseAnalyzerFacadeForJVM.createJavaModule("<module>");
        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        module.seal();
        
        return EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                javaProject, 
                kotlinEnvironment.getProject(), 
                filesToAnalyze,
                module);
    }
}