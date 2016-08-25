/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IResourceDeltaVisitor
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.debug.core.model.LaunchConfigurationDelegate
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.compiler.KotlinCompiler.KotlinCompilerResult
import org.jetbrains.kotlin.core.compiler.KotlinCompilerUtils
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil
import com.google.common.collect.Sets
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.core.resources.IMarker
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.progress.CompilationCanceledException
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration
import org.jetbrains.kotlin.ui.KotlinPluginUpdater
import org.jetbrains.kotlin.eclipse.ui.utils.runJob
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment

class KotlinBuilder : IncrementalProjectBuilder() {
    override fun build(kind: Int, args: Map<String, String>?, monitor: IProgressMonitor?): Array<IProject>? {
        val javaProject = JavaCore.create(project)
        if (isBuildingForLaunch()) {
            compileKotlinFiles(javaProject)
            return null
        }
        
        val delta = getDelta(project)
        val allAffectedFiles = if (delta != null) getAllAffectedFiles(delta) else emptySet()
        if (allAffectedFiles.isNotEmpty() &&
                (isAllFromOutputFolder(allAffectedFiles, javaProject) || isAllScripts(allAffectedFiles))) {
            return null
        }
        
        val kotlinAffectedFiles = if (kind == FULL_BUILD) {
            KotlinPsiManager.getFilesByProject(project)
        } else {
            allAffectedFiles
                .filter { KotlinPsiManager.isKotlinSourceFile(it, javaProject) }
                .toSet()
        }
        
        val existingAffectedFiles = kotlinAffectedFiles.filter { it.exists() }
        
        commitFiles(existingAffectedFiles)
        
        KotlinLightClassGeneration.updateLightClasses(javaProject.project, kotlinAffectedFiles)
        if (kotlinAffectedFiles.isNotEmpty()) {
            
            runJob("Checking for update", Job.DECORATE) { 
                KotlinPluginUpdater.kotlinFileEdited()
                Status.OK_STATUS
            }
        }
        
        val ktFiles = existingAffectedFiles.map { KotlinPsiManager.getParsedFile(it) }
        
        val analysisResultWithProvider = if (ktFiles.isEmpty())
            KotlinAnalyzer.analyzeProject(project)
        else
            KotlinAnalyzer.analyzeFiles(ktFiles)
                
        clearProblemAnnotationsFromOpenEditorsExcept(existingAffectedFiles)
        updateLineMarkers(analysisResultWithProvider.analysisResult.bindingContext.diagnostics, existingAffectedFiles)
        
        runCancellableAnalysisFor(javaProject, existingAffectedFiles)
        
        return null
    }
    
    private fun commitFiles(files: Collection<IFile>) {
        files.forEach { KotlinPsiManager.commitFile(it, EditorUtil.getDocument(it)) }
    }
    
    private fun isAllScripts(files: Set<IFile>): Boolean {
        return files.all { KotlinScriptEnvironment.isScript(it) }
    }
    
    private fun isAllFromOutputFolder(files: Set<IFile>, javaProject: IJavaProject): Boolean {
        val workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getFullPath()
        val outputLocation = javaProject.outputLocation
        for (file in files) {
            val filePathLocation = file.getFullPath().makeRelativeTo(workspaceLocation)
            if (!outputLocation.isPrefixOf(filePathLocation)) {
                return false
            }
        }
        
        return true
    }
    
    private fun getAllAffectedFiles(resourceDelta: IResourceDelta): Set<IFile> {
        val affectedFiles = hashSetOf<IFile>()
        resourceDelta.accept { delta ->
            if (delta.getKind() == IResourceDelta.NO_CHANGE) return@accept false
                
            val resource = delta.getResource()
            if (resource is IFile) {
                affectedFiles.add(resource)
            } else {
                return@accept true
            }
            
            false
        }
        
        return affectedFiles
    }
    
    private fun isBuildingForLaunch(): Boolean {
        val launchDelegateFQName = LaunchConfigurationDelegate::class.java.getCanonicalName()
        return Thread.currentThread().getStackTrace().find { it.className == launchDelegateFQName } != null
    }
    
    private fun compileKotlinFiles(javaProject: IJavaProject) {
        val compilerResult = KotlinCompilerUtils.compileWholeProject(javaProject)
        if (!compilerResult.compiledCorrectly()) {
            KotlinCompilerUtils.handleCompilerOutput(compilerResult.getCompilerOutput())
        }
    }
}

fun updateLineMarkers(diagnostics: Diagnostics, affectedFiles: List<IFile>) {
    clearMarkersFromFiles(affectedFiles)
    addMarkersToProject(DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics), affectedFiles)
}

private fun clearMarkersFromFiles(files: List<IFile>) {
    files.forEach { it.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE) }
}

private fun clearProblemAnnotationsFromOpenEditorsExcept(affectedFiles: List<IFile>) {
    for (window in PlatformUI.getWorkbench().getWorkbenchWindows()) {
        for (page in window.getPages()) {
            page.getEditorReferences()
                .map { it.getEditor(false) }
                .filterIsInstance(KotlinFileEditor::class.java)
                .filterNot { it.eclipseFile in affectedFiles }
                .forEach { 
                    AnnotationManager.removeAnnotations(it, AnnotationManager.ANNOTATION_ERROR_TYPE)
                    AnnotationManager.removeAnnotations(it, AnnotationManager.ANNOTATION_WARNING_TYPE)
                }
        }
    }
}

private fun addMarkersToProject(annotations: Map<IFile, List<DiagnosticAnnotation>>, affectedFiles: List<IFile>) {
    for (file in affectedFiles) {
        DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(file, annotations)
        annotations[file]?.forEach { AnnotationManager.addProblemMarker(it, file) }
    }
}