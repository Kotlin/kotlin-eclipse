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
package org.jetbrains.kotlin.ui.editors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class AnnotationManager {
    
    public static final String MARKER_TYPE = "org.jetbrains.kotlin.ui.marker";
    public static final String ANNOTATION_ERROR_TYPE = "org.jetbrains.kotlin.ui.annotation.error";
    public static final String ANNOTATION_WARNING_TYPE = "org.jetbrains.kotlin.ui.annotation.warning";
    public static final String MARKED_TEXT = "markedText";
    public static final String IS_UNRESOLVED_REFERENCE = "isUnresolvedReference";
    
    public static final String MARKER_PROBLEM_TYPE = IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER;
    
    public static void updateAnnotations(
            @NotNull AbstractTextEditor editor, 
            @NotNull List<DiagnosticAnnotation> annotations,
            @NotNull Predicate<Annotation> replacementAnnotationsPredicate) throws CoreException {
        IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        if (!(annotationModel instanceof IAnnotationModelExtension)) {
            return; 
        }
        
        Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
        for (DiagnosticAnnotation annotation : annotations) {
            newAnnotations.put(annotation, annotation.getPosition());
        }
        
        List<Annotation> oldAnnotations = getAnnotations(annotationModel, replacementAnnotationsPredicate);
        
        IAnnotationModelExtension modelExtension = (IAnnotationModelExtension) annotationModel;
        modelExtension.replaceAnnotations(oldAnnotations.toArray(new Annotation[oldAnnotations.size()]), newAnnotations);
    }
    
    public static void clearAllMarkersFromProject(@NotNull IJavaProject javaProject) {
        IProject project = javaProject.getProject();
        for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            try {
                file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            } catch (CoreException e) {
                KotlinLogger.logError(e);
            }
        }
    }
    
    public static void addProblemMarker(@NotNull DiagnosticAnnotation annotation, @NotNull IFile file) {
        try {
            IMarker problemMarker = file.createMarker(MARKER_PROBLEM_TYPE);
            problemMarker.setAttribute(IMarker.MESSAGE, annotation.getText());
            problemMarker.setAttribute(IMarker.SEVERITY, annotation.getMarkerSeverity());
            problemMarker.setAttribute(IMarker.CHAR_START, annotation.getRange().getStartOffset());
            problemMarker.setAttribute(IMarker.CHAR_END, annotation.getRange().getEndOffset());
            problemMarker.setAttribute(MARKED_TEXT, annotation.getMarkedText());
            
            DiagnosticFactory<?> diagnostic = annotation.getDiagnostic();
            boolean isUnresolvedReference = diagnostic != null ? DiagnosticAnnotationUtil.isUnresolvedReference(diagnostic) : false;
            problemMarker.setAttribute(IS_UNRESOLVED_REFERENCE, isUnresolvedReference); 
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    private static List<Annotation> getAnnotations(@NotNull IAnnotationModel model, Predicate<Annotation> predicate) {
        Iterator<?> annotationIterator = model.getAnnotationIterator();
        List<Annotation> oldAnnotations = Lists.newArrayList();
        while (annotationIterator.hasNext()) {
            Annotation annotation = (Annotation) annotationIterator.next();
            if (predicate.apply(annotation)) {
                oldAnnotations.add(annotation);
            }
        }
        
        return oldAnnotations;
    }
}