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
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration
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

class KotlinBuilder : IncrementalProjectBuilder() {
    override fun build(kind: Int, args: Map<String, String>?, monitor: IProgressMonitor?): Array<IProject>? {
        val javaProject = JavaCore.create(project)
        if (isBuildingForLaunch()) {
            compileKotlinFiles(javaProject)
            return null
        }
        
        val affectedFiles = if (kind == FULL_BUILD) {
            KotlinPsiManager.INSTANCE.getFilesByProject(project)
        } else {
            val delta = getDelta(project)
            if (delta != null) getAffectedFiles(delta, javaProject) else emptySet()
        }
        
        val existingAffectedFiles = affectedFiles.filter { it.exists() }
        
        commitFiles(existingAffectedFiles)
        
        if (affectedFiles.isNotEmpty()) {
            KotlinLightClassGeneration.updateLightClasses(javaProject, affectedFiles)
        }
        
        val ktFiles = existingAffectedFiles.map { KotlinPsiManager.INSTANCE.getParsedFile(it) }
        val analysisResult = KotlinAnalyzer.analyzeFiles(javaProject, ktFiles).analysisResult
        updateLineMarkers(analysisResult.bindingContext.diagnostics, existingAffectedFiles)
        
        runCancellableAnalysisFor(javaProject, existingAffectedFiles)
        
        return null
    }
    
    private fun commitFiles(files: Collection<IFile>) {
        files.forEach { KotlinPsiManager.getKotlinFileIfExist(it, EditorUtil.getDocument(it).get()) }
    }
    
    private fun getAffectedFiles(resourceDelta: IResourceDelta, javaProject: IJavaProject): Set<IFile> {
        val affectedFiles = hashSetOf<IFile>()
        resourceDelta.accept { delta ->
            if (delta.getKind() != IResourceDelta.NO_CHANGE) {
                val resource = delta.getResource()
                if (KotlinPsiManager.INSTANCE.isKotlinSourceFile(resource, javaProject)) {
                    affectedFiles.add(resource as IFile)
                    return@accept false
                }
                
                if (resource !is IFile) return@accept true
            }
            
            false
        }
        
        return affectedFiles
    }
    
    private fun isBuildingForLaunch():Boolean {
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

private fun addMarkersToProject(annotations: Map<IFile, List<DiagnosticAnnotation>>, affectedFiles: List<IFile>) {
    affectedFiles.forEach { DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(it, annotations) }
    
    for (file in affectedFiles) {
        annotations[file]?.forEach { AnnotationManager.addProblemMarker(it, file) }
    }
}