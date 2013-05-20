package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jetbrains.kotlin.core.builder.KotlinManager;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.openapi.util.TextRange;

public class AnnotationManager {
    
    public static final String markerType = "org.jetbrains.kotlin.ui.marker";
    public static final String annotationErrorType = "org.jetbrains.kotlin.ui.annotation.error";
    public static final String annotationWarningType = "org.jetbrains.kotlin.ui.annotation.warning";
    
    public static void updateAnnotations(TextEditor editor, List<KotlinAnnotation> annotations) {
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        IDocument document = documentProvider.getDocument(editor.getEditorInput());
        
        IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput());
        
        if (annotationModel instanceof IAnnotationModelExtension) {
            IAnnotationModelExtension modelExtension = (IAnnotationModelExtension) annotationModel;
            Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
            for (KotlinAnnotation annotation : annotations) {
                newAnnotations.put(annotation, annotation.getPosition());
            }
            List<Annotation> oldAnnotations = new ArrayList<Annotation>();
            for (Iterator<?> i = annotationModel.getAnnotationIterator(); i.hasNext();) {
                oldAnnotations.add((Annotation) i.next());
            }
            
            modelExtension.replaceAnnotations(oldAnnotations.toArray(new Annotation[oldAnnotations.size()]), newAnnotations);
            
            return;
        }
        
        annotationModel.connect(document);
        for (Iterator<?> i = annotationModel.getAnnotationIterator(); i.hasNext();) {
            annotationModel.removeAnnotation((Annotation) i.next());
        }
        for (KotlinAnnotation annotation : annotations) {
            annotationModel.addAnnotation(annotation, annotation.getPosition());
        }
        annotationModel.disconnect(document);
    }
    
    public static void clearAllMarkers() {
        List<IFile> workspaceFiles = new ArrayList<IFile>(KotlinManager.getAllFiles());
        for (IFile file : workspaceFiles) {
            try {
                file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            } catch (CoreException e) {
            }
        }
    }
    
    public static void addProblemMarker(IFile file, String message, int problemSeverity, String fileText, TextRange range) {
        try {
            IMarker problemMarker = file.createMarker(IMarker.PROBLEM);
            problemMarker.setAttribute(IMarker.MESSAGE, message);
            problemMarker.setAttribute(IMarker.SEVERITY, problemSeverity);
            
            int start = LineEndUtil.convertLfToOsOffset(fileText, range.getStartOffset());
            problemMarker.setAttribute(IMarker.CHAR_START, start);
            
            int end = LineEndUtil.convertLfToOsOffset(fileText, range.getEndOffset());
            problemMarker.setAttribute(IMarker.CHAR_END, end);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
}