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
package org.jetbrains.kotlin.ui.editors.annotations

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
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.ui.editors.KotlinReconcilingListener
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor

public object AnnotationManager {
    val MARKER_TYPE = "org.jetbrains.kotlin.ui.marker"
    
    @JvmField val ANNOTATION_ERROR_TYPE = "org.jetbrains.kotlin.ui.annotation.error"
    @JvmField val ANNOTATION_WARNING_TYPE = "org.jetbrains.kotlin.ui.annotation.warning"
    val MARKED_TEXT = "markedText"
    @JvmField val IS_UNRESOLVED_REFERENCE = "isUnresolvedReference"
    @JvmField val MARKER_PROBLEM_TYPE = IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER
    
    public fun updateAnnotations(editor: AbstractTextEditor, annotations: List<DiagnosticAnnotation>) {
        val annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput())
        if (annotationModel !is IAnnotationModelExtension) return
        
        val newAnnotations = annotations.toMapBy({ it }, { it.position })
        val oldAnnotations = getLineMarkerAnnotations(annotationModel)
        updateAnnotations<DiagnosticAnnotation>(annotationModel, newAnnotations, oldAnnotations)
    }
    
    public fun clearAllMarkersFromProject(project: IProject) {
        try {
            KotlinPsiManager.INSTANCE.getFilesByProject(project).forEach { 
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
            setAttribute(IMarker.SEVERITY, annotation.markerSeverity)
            setAttribute(IMarker.CHAR_START, annotation.offset)
            setAttribute(IMarker.CHAR_END, annotation.endOffset)
            setAttribute(MARKED_TEXT, annotation.markedText)
            
            val diagnostic = annotation.diagnostic
            val isUnresolvedReference = if (diagnostic != null) DiagnosticAnnotationUtil.isUnresolvedReference(diagnostic) else false
            setAttribute(IS_UNRESOLVED_REFERENCE, isUnresolvedReference)
        }
    }
    
    fun updateAnnotations(editor: KotlinFileEditor, annotationMap: Map<Annotation, Position>, annotationType: String) {
        val model = editor.getDocumentProvider()?.getAnnotationModel(editor.getEditorInput())
        if (model != null) {
            updateAnnotations(model, annotationMap, getAnnotations(model, annotationType))
        }
    }
    
    fun getAnnotations(model: IAnnotationModel, annontationType: String): List<Annotation> {
        val annotations = arrayListOf<Annotation>()
        for (annotation in model.getAnnotationIterator()) {
            if (annotation is Annotation && annotation.getType() == annontationType) {
                annotations.add(annotation)
            }
        }
        
        return annotations
    }
    
    private fun <Ann : Annotation> updateAnnotations(
            model: IAnnotationModel, 
            annotationMap: Map<Ann, Position>, 
            oldAnnotations: List<Annotation>) {
        model.withLock { 
            (model as IAnnotationModelExtension).replaceAnnotations(oldAnnotations.toTypedArray(), annotationMap)
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

object KotlinLineAnnotationsReconciler : KotlinReconcilingListener {
    override fun reconcile(file: IFile, editor: KotlinFileEditor) {
        val jetFile = KotlinPsiManager.getKotlinFileIfExist(file, EditorUtil.getSourceCode(editor))
        if (jetFile == null) {
            return
        }
        
        val diagnostics = KotlinAnalyzer.analyzeFile(editor.javaProject!!, jetFile).analysisResult.bindingContext.diagnostics
        val annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics)
        
        DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(file, annotations)
        DiagnosticAnnotationUtil.INSTANCE.updateAnnotations(editor, annotations)
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