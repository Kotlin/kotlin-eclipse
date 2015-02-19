/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.builder;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.compiler.KotlinCompiler.KotlinCompilerResult;
import org.jetbrains.kotlin.core.compiler.KotlinCompilerUtils;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.ui.editors.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotation;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotationUtil;

public class KotlinBuilder extends IncrementalProjectBuilder {

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        IJavaProject javaProject = JavaCore.create(getProject());
        if (isBuildingForLaunch()) {
            compileKotlinFiles(javaProject);
        }
        
        AnalysisResult analysisResult = KotlinAnalyzer.analyzeWholeProject(javaProject);
        updateLineMarkers(analysisResult.getBindingContext().getDiagnostics());
        
        boolean needRebuild = false;
        if (kind == FULL_BUILD) {
            needRebuild = true;
        } else {
            IResourceDelta delta = getDelta(getProject());
            if (delta != null) {
                needRebuild = delta.getKind() != IResourceDelta.NO_CHANGE;
            }
        }
        
        if (needRebuild) {
            KotlinLightClassGeneration.buildAndSaveLightClasses(analysisResult, javaProject);
            getProject().refreshLocal(0, null);
        }
        
        return null;
    }
    
    private boolean isBuildingForLaunch() {
        String launchDelegateFQName = LaunchConfigurationDelegate.class.getCanonicalName();
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (launchDelegateFQName.equals(stackTraceElement.getClassName())) {
                return true;
            }
        }
        
        return false;
    }
    
    private void compileKotlinFiles(@NotNull IJavaProject javaProject) throws CoreException {
        KotlinCompilerResult compilerResult = KotlinCompilerUtils.compileWholeProject(javaProject);
        if (!compilerResult.compiledCorrectly()) {
            KotlinCompilerUtils.handleCompilerOutput(compilerResult.getCompilerOutput());
        }
    }
    
    private void updateLineMarkers(@NotNull Diagnostics diagnostics) throws CoreException {
        addMarkersToProject(DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics), getProject());
    }
    
    private void addMarkersToProject(Map<IFile, List<DiagnosticAnnotation>> annotations, IProject project) throws CoreException {
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            if (file.exists()) {
                file.deleteMarkers(AnnotationManager.MARKER_PROBLEM_TYPE, true, IResource.DEPTH_INFINITE);
            }
        }
        
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(getProject())) {
            DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(file, annotations);
        }
        
        for (Map.Entry<IFile, List<DiagnosticAnnotation>> entry : annotations.entrySet()) {
            for (DiagnosticAnnotation annotation : entry.getValue()) {
                AnnotationManager.addProblemMarker(annotation, entry.getKey());
            }
        }
    }
}
