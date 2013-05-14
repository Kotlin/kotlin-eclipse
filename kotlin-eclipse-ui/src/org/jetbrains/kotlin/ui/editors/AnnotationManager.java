package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class AnnotationManager {
    
    public static final String markerType = "org.jetbrains.kotlin.ui.marker";
    public static final String annotationErrorType = "org.jetbrains.kotlin.ui.annotation.error";
    
    public static void updateAnnotations(TextEditor editor, List<KotlinAnnotation> annotations) {
        if (annotations == null) {
            annotations = new LinkedList<KotlinAnnotation>();
        }
        
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
            for (Iterator i = annotationModel.getAnnotationIterator(); i.hasNext();) {
                oldAnnotations.add((Annotation) i.next());
            }
            Annotation[] oldArray = new Annotation[oldAnnotations.size()];
            for (int i = 0; i < oldAnnotations.size(); ++i) {
                oldArray[i] = oldAnnotations.get(i);
            }
            
            modelExtension.replaceAnnotations(oldArray, newAnnotations);
            
            return;
        }
        
        annotationModel.connect(document);
        for (Iterator i = annotationModel.getAnnotationIterator(); i.hasNext();) {
            annotationModel.removeAnnotation((Annotation) i.next());
        }
        for (KotlinAnnotation annotation : annotations) {
            annotationModel.addAnnotation(annotation, annotation.getPosition());
        }
        annotationModel.disconnect(document);
    }
}
