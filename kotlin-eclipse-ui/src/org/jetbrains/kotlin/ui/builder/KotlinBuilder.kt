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

class KotlinBuilder : IncrementalProjectBuilder() {
    override fun build(kind: Int, args: Map<String, String>?, monitor: IProgressMonitor?): Array<IProject>? {
        val project = getProject()
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
        
        commitFiles(affectedFiles)
        
        if (affectedFiles.isNotEmpty()) {
            KotlinLightClassGeneration.updateLightClasses(javaProject, affectedFiles)
        }
        
        val analysisResult = KotlinAnalysisProjectCache.getAnalysisResult(javaProject)
        updateLineMarkers(analysisResult.bindingContext.diagnostics)
        
        return null
    }
    
    private fun commitFiles(files: Collection<IFile>) {
        for (file in files) {
            if (file.exists()) {
                KotlinPsiManager.getKotlinFileIfExist(file, EditorUtil.getDocument(file).get())
            }
        }
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
    
    private fun compileKotlinFiles(javaProject:IJavaProject) {
        val compilerResult = KotlinCompilerUtils.compileWholeProject(javaProject)
        if (!compilerResult.compiledCorrectly()) {
            KotlinCompilerUtils.handleCompilerOutput(compilerResult.getCompilerOutput())
        }
    }
    
    private fun updateLineMarkers(diagnostics:Diagnostics) {
        addMarkersToProject(DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics), getProject())
    }
    
    private fun addMarkersToProject(annotations: Map<IFile, List<DiagnosticAnnotation>>, project: IProject) {
        AnnotationManager.clearAllMarkersFromProject(project)
        for (file in KotlinPsiManager.INSTANCE.getFilesByProject(getProject())) {
            DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(file, annotations)
        }
        
        for ((file, diagnosticAnnotations) in annotations) {
            for (annotation in diagnosticAnnotations) {
                AnnotationManager.addProblemMarker(annotation, file)
            }
        }
    }
}