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

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.CoreException
import org.eclipse.jdt.core.IJavaModelMarker
import org.eclipse.jface.text.ISynchronizable
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.jface.text.source.IAnnotationModelExtension
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.eclipse.ui.texteditor.MarkerAnnotation
import org.eclipse.ui.texteditor.MarkerUtilities
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.KotlinReconcilingListener
import org.jetbrains.kotlin.ui.editors.quickfix.addDiagnostics
import org.jetbrains.kotlin.ui.editors.quickfix.kotlinQuickFixes
import org.jetbrains.kotlin.ui.editors.quickfix.removeMarkers

object AnnotationManager {

    const val ANNOTATION_ERROR_TYPE = "org.jetbrains.kotlin.ui.annotation.error"
    const val ANNOTATION_WARNING_TYPE = "org.jetbrains.kotlin.ui.annotation.warning"
    const val MARKED_TEXT = "markedText"
    @JvmField
    val IS_UNRESOLVED_REFERENCE = "isUnresolvedReference"
    @JvmField
    val MARKER_PROBLEM_TYPE = IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER
    val CAN_FIX_PROBLEM = "KotlinProblemCanBeFixed"

    fun updateAnnotations(editor: AbstractTextEditor, annotations: List<DiagnosticAnnotation>) {
        val annotationModel = editor.documentProvider.getAnnotationModel(editor.editorInput)
        if (annotationModel !is IAnnotationModelExtension) return

        val newAnnotations = annotations.associateBy({ it }, { it.position })
        val oldAnnotations = getLineMarkerAnnotations(annotationModel)
        updateAnnotations(annotationModel, newAnnotations, oldAnnotations)
    }

    fun clearAllMarkersFromProject(project: IProject) {
        try {
            KotlinPsiManager.getFilesByProject(project).forEach {
                it.removeMarkers()
            }
        } catch (e: CoreException) {
            KotlinLogger.logError(e)
        }
    }

    fun addProblemMarker(annotation: DiagnosticAnnotation, file: IFile) =
        with(file.createMarker(MARKER_PROBLEM_TYPE)) {
            setAttribute(IMarker.MESSAGE, annotation.text)
            setAttribute(IMarker.SEVERITY, annotation.markerSeverity)
            setAttribute(IMarker.CHAR_START, annotation.offset)
            setAttribute(IMarker.CHAR_END, annotation.endOffset)
            setAttribute(IMarker.LOCATION, "line ${annotation.line}")
            setAttribute(IMarker.LINE_NUMBER, annotation.line)
            setAttribute(MARKED_TEXT, annotation.markedText)
            annotation.diagnostic?.let {
                addDiagnostics(it)
            }

            val diagnostic = annotation.diagnostic
            val isUnresolvedReference = if (diagnostic != null) {
                DiagnosticAnnotationUtil.isUnresolvedReference(diagnostic.factory)
            } else false
            setAttribute(IS_UNRESOLVED_REFERENCE, isUnresolvedReference)

            val canBeFixed = diagnostic?.let { kotlinQuickFixes.containsKey(it.factory) } ?: false
            setAttribute(CAN_FIX_PROBLEM, canBeFixed)
        }

    fun removeAnnotations(editor: KotlinFileEditor, annotationType: String) {
        updateAnnotations(editor, emptyMap(), annotationType)
    }

    fun updateAnnotations(editor: KotlinEditor, annotationMap: Map<Annotation, Position>, annotationType: String) {
        val model = editor.javaEditor.documentProvider?.getAnnotationModel(editor.javaEditor.editorInput)
        if (model != null) {
            updateAnnotations(model, annotationMap, getAnnotations(model, annotationType))
        }
    }

    private fun getAnnotations(model: IAnnotationModel, annontationType: String): List<Annotation> {
        val annotations = arrayListOf<Annotation>()
        for (annotation in model.annotationIterator) {
            if (annotation is Annotation && annotation.type == annontationType) {
                annotations.add(annotation)
            }
        }

        return annotations
    }

    private fun <Ann : Annotation> updateAnnotations(
        model: IAnnotationModel,
        annotationMap: Map<Ann, Position>,
        oldAnnotations: List<Annotation>
    ) {
        model.withLock {
            (model as IAnnotationModelExtension).replaceAnnotations(oldAnnotations.toTypedArray(), annotationMap)
        }
    }

    private fun getLineMarkerAnnotations(model: IAnnotationModel): List<Annotation> {
        fun isLineMarkerAnnotation(ann: Annotation): Boolean {
            return when (ann) {
                is DiagnosticAnnotation -> true
                is MarkerAnnotation -> MarkerUtilities.isMarkerType(ann.marker, IMarker.PROBLEM)
                else -> false
            }
        }

        return arrayListOf<Annotation>().apply {
            model.annotationIterator.forEach {
                if (it is Annotation && isLineMarkerAnnotation(it)) {
                    add(it)
                }
            }
        }
    }
}

object KotlinLineAnnotationsReconciler : KotlinReconcilingListener {
    override fun reconcile(file: IFile, editor: KotlinEditor) {
        val jetFile =
            if (editor.isScript) editor.parsedFile else KotlinPsiManager.getKotlinFileIfExist(file, editor.document.get())

        jetFile?.let {
            val diagnostics = KotlinAnalyzer.analyzeFile(it).analysisResult.bindingContext.diagnostics
            val annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics)

            DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(file, annotations)
            DiagnosticAnnotationUtil.INSTANCE.updateAnnotations(editor.javaEditor, annotations)
        }
    }
}

fun <T> IAnnotationModel.withLock(action: () -> T): T {
    return if (this is ISynchronizable) {
        synchronized(this.lockObject) {
            action()
        }
    } else {
        synchronized(this) {
            action()
        }
    }
}
