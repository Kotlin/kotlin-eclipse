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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.descriptors.serialization.descriptors.MemberFilter;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

public class KotlinAnalyzer {

    @NotNull
    public static BindingContext analyzeProject(@NotNull IJavaProject javaProject) {
        KotlinEnvironment kotlinEnvironment = KotlinEnvironment.getEnvironment(javaProject);
        return analyzeProject(javaProject, kotlinEnvironment, Predicates.<PsiFile>alwaysTrue());
    }
    
    @NotNull
    public static BindingContext analyzeOnlyOneFileCompletely(@NotNull IJavaProject javaProject, @NotNull PsiFile psiFile) {
        KotlinEnvironment kotlinEnvironment = KotlinEnvironment.getEnvironment(javaProject);
        return analyzeProject(javaProject, kotlinEnvironment, Predicates.equalTo(psiFile));
    }
    
    @NotNull
    private static BindingContext analyzeProject(@NotNull IJavaProject javaProject, @NotNull KotlinEnvironment kotlinEnvironment, @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        Project ideaProject = kotlinEnvironment.getProject();
        
        List<JetFile> sourceFiles = getSourceFiles(javaProject.getProject());
        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                ideaProject, sourceFiles, new BindingTraceContext(), filesToAnalyzeCompletely, AnalyzerFacadeForJVM.createJavaModule("<module>"), MemberFilter.ALWAYS_TRUE);
        
        return analyzeExhaust.getBindingContext();
    }
    
    @NotNull
    public static List<JetFile> getSourceFiles(@NotNull IProject project) {
        List<JetFile> jetFiles = new ArrayList<JetFile>();
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
            jetFiles.add(jetFile);
         }
        
        return jetFiles;
    }
}