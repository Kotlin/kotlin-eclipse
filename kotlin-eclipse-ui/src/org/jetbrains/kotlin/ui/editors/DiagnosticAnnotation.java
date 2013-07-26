package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;

import com.intellij.openapi.util.TextRange;

public class DiagnosticAnnotation extends Annotation {

    private final TextRange range;
    
    public DiagnosticAnnotation(TextRange position, String annotationType, String message) {
        super(annotationType, true, message);
        this.range = position;
    }
    
    public DiagnosticAnnotation(int offset, int length, String annotationType, String message) {
        this(new TextRange(offset, offset + length), annotationType, message);
    }
    
    public TextRange getRange() {
        return range;
    }
    
    public int getMarkerSeverity() {
        switch (getType()) {
            case AnnotationManager.ANNOTATION_ERROR_TYPE:
                return IMarker.SEVERITY_ERROR;
                
            case AnnotationManager.ANNOTATION_WARNING_TYPE:
                return IMarker.SEVERITY_WARNING;
                
            default:
                return 0;
        }
    }
    
    public Position getPosition() {
        return new Position(range.getStartOffset(), range.getLength());
    }
}