package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;

import com.intellij.openapi.util.TextRange;

public class AnnotationManager {
    
    public static final String markerType = "org.jetbrains.kotlin.marker";
    public static final String annotationErrorType = "org.jetbrains.kotlin.annotation.error";
    
    public static void addAnnotations(List<KotlinAnnotation> annotations, TextEditor editor) {
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        
        IDocument document = documentProvider.getDocument(editor.getEditorInput());
        
        IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput());
        
        annotationModel.connect(document);
        for (KotlinAnnotation annotation : annotations) {
            annotationModel.addAnnotation(annotation, annotation.getPosition());
        }
        annotationModel.disconnect(document);
    } 
    
    public static void addAnnotationsToFile(IFile file, TextEditor editor) {
        BindingContext bindingContext = KotlinAnalyzer.analyze();
        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>(bindingContext.getDiagnostics());
        List<KotlinAnnotation> annotations = new LinkedList<KotlinAnnotation>();
        for (Diagnostic diagnostic : diagnostics) {
            if (!diagnostic.getPsiFile().getVirtualFile().getPath().endsWith(file.getFullPath().toString())) {
                continue;
            }
            List<TextRange> ranges = diagnostic.getTextRanges();
            if (ranges.isEmpty()) {
                continue;
            }
            switch (diagnostic.getSeverity()) {
                case ERROR:
                    annotations.add(new KotlinAnnotation(ranges.get(0).getStartOffset(), 
                            ranges.get(0).getLength(), annotationErrorType));
                    break;
                default:
                    continue;
            }
        }

        addAnnotations(annotations, editor);
    }
    
    public static void updateAnnotations(IFile file, TextEditor editor, List<KotlinAnnotation> annotations) {
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        IDocument document = documentProvider.getDocument(editor.getEditorInput());
        
        IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput());
        
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
