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
package org.jetbrains.kotlin.ui.editors

import java.util.HashMap
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.CoreException
import org.eclipse.jdt.core.IJavaModelMarker
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.jface.text.source.IAnnotationModelExtension
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import com.google.common.base.Predicate
import com.google.common.collect.Lists
import org.eclipse.ui.texteditor.MarkerAnnotation
import org.eclipse.ui.texteditor.MarkerUtilities
import org.eclipse.jface.text.ISynchronizable

public object AnnotationManager {
    val MARKER_TYPE = "org.jetbrains.kotlin.ui.marker"
    val ANNOTATION_ERROR_TYPE = "org.jetbrains.kotlin.ui.annotation.error"
    val ANNOTATION_WARNING_TYPE = "org.jetbrains.kotlin.ui.annotation.warning"
    val MARKED_TEXT = "markedText"
    val IS_UNRESOLVED_REFERENCE = "isUnresolvedReference"
    val MARKER_PROBLEM_TYPE = IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER
    
    public fun updateAnnotations(editor: AbstractTextEditor, annotations: List<DiagnosticAnnotation>) {
        val annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput())
        if (annotationModel !is IAnnotationModelExtension) return
        
        val newAnnotations = annotations.toMap({ it }, { it.getPosition() })
        val oldAnnotations = getLineMarkerAnnotations(annotationModel)
        annotationModel.withLock<Unit> { 
            annotationModel.replaceAnnotations(oldAnnotations.toTypedArray(), newAnnotations)
        }
    }
    
    public fun clearAllMarkersFromProject(javaProject: IJavaProject) {
        try {
            KotlinPsiManager.INSTANCE.getFilesByProject(javaProject.getProject()).forEach { 
                it.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE) 
            }
        } catch (e: CoreException) {
            KotlinLogger.logError(e)
        }
    }
    
    public fun addProblemMarker(annotation: DiagnosticAnnotation, file: IFile) {
        val problemMarker = file.createMarker(MARKER_PROBLEM_TYPE)
        with(problemMarker) {
            setAttribute(IMarker.MESSAGE, annotation.getText())
            setAttribute(IMarker.SEVERITY, annotation.getMarkerSeverity())
            setAttribute(IMarker.CHAR_START, annotation.getRange().getStartOffset())
            setAttribute(IMarker.CHAR_END, annotation.getRange().getEndOffset())
            setAttribute(MARKED_TEXT, annotation.getMarkedText())
            
            val diagnostic = annotation.getDiagnostic()
            val isUnresolvedReference = if (diagnostic != null) DiagnosticAnnotationUtil.isUnresolvedReference(diagnostic) else false
            setAttribute(IS_UNRESOLVED_REFERENCE, isUnresolvedReference)
        }
    }
    
    private fun getLineMarkerAnnotations(model: IAnnotationModel): List<Annotation> {
        fun isLineMarkerAnnotation(ann: Annotation): Boolean {
            return when (ann) {
                is DiagnosticAnnotation -> true
                is MarkerAnnotation -> MarkerUtilities.isMarkerType(ann.getMarker(), IMarker.PROBLEM)
                else -> false
            }
        }
        
        val annotations = arrayListOf<Annotation>()
        model.getAnnotationIterator().forEach { 
            if (it is Annotation && isLineMarkerAnnotation(it)) {
                annotations.add(it)
            }
        }
        
        return annotations
    }
}

fun <T> IAnnotationModel.withLock(action: () -> T): T {
    return if (this is ISynchronizable) {
        synchronized (this.getLockObject()) {
            action()
        }
    } else {
        synchronized (this) {
            action()
        }
    }
}