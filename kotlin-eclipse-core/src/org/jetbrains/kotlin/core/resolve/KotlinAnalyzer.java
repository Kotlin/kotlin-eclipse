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
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.psi.PsiFile;

public class KotlinAnalyzer {

    @NotNull
    public static AnalyzeExhaust analyzeDeclarations(@NotNull IJavaProject javaProject) {
        return analyzeProject(javaProject, Predicates.<PsiFile>alwaysFalse());
    }
    
    @NotNull
    public static AnalyzeExhaust analyzeWholeProject(@NotNull IJavaProject javaProject) {
        return analyzeProject(javaProject, Predicates.<PsiFile>alwaysTrue());
    }

    @NotNull
    public static AnalyzeExhaust analyzeOneFileCompletely(@NotNull IJavaProject javaProject, @NotNull PsiFile psiFile) {
        return analyzeProject(javaProject, Predicates.equalTo(psiFile));
    }
    
    private static AnalyzeExhaust analyzeProject(@NotNull IJavaProject javaProject, @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        KotlinEnvironment kotlinEnvironment = KotlinEnvironment.getEnvironment(javaProject);
        return analyzeExhaustProject(javaProject, kotlinEnvironment, filesToAnalyzeCompletely);
    }
    
    @NotNull
    private static AnalyzeExhaust analyzeExhaustProject(@NotNull IJavaProject javaProject, @NotNull KotlinEnvironment kotlinEnvironment, 
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        return EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                javaProject, 
                kotlinEnvironment.getProject(), 
                getSourceFiles(javaProject.getProject()), 
                new BindingTraceContext(), 
                filesToAnalyzeCompletely, 
                AnalyzerFacadeForJVM.createJavaModule("<module>"));
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