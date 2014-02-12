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
package org.jetbrains.kotlin.ui.builder;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.AnalyzerScheduler;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;
import org.jetbrains.kotlin.ui.editors.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotation;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotationUtil;

public class KotlinBuilder extends IncrementalProjectBuilder {

    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        KotlinEnvironment.updateKotlinEnvironment(JavaCore.create(getProject()));
        updateLineMarkers();
        
        return null;
    }

    private void updateLineMarkers() throws CoreException {
        Diagnostics diagnostics = analyzeProjectInForeground(JavaCore.create(getProject())).getDiagnostics();
        Map<IFile, List<DiagnosticAnnotation>> annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics);

        addMarkersToProject(annotations, getProject());
    }
    
    @NotNull
    public static BindingContext analyzeProjectInForeground(IJavaProject javaProject) {
        AnalyzerScheduler analyzer = new AnalyzerScheduler(javaProject);
        analyzer.cancel();
        analyzer.schedule();
        
        IJobManager jobManager = Job.getJobManager();
        try {
            jobManager.join(AnalyzerScheduler.FAMILY, null);
        } catch (OperationCanceledException | InterruptedException e) {
            KotlinLogger.logInfo(e.getMessage());
            
            return BindingContext.EMPTY;
        }
        
        return analyzer.getBindingContext();
    }
    
    private void addMarkersToProject(Map<IFile, List<DiagnosticAnnotation>> annotations, IProject project) throws CoreException {
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            if (file.exists()) {
                file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            }
        }
        
        for (Map.Entry<IFile, List<DiagnosticAnnotation>> entry : annotations.entrySet()) {
            for (DiagnosticAnnotation annotation : entry.getValue()) {
                AnnotationManager.addProblemMarker(annotation, entry.getKey());
            }
        }
    }
}
