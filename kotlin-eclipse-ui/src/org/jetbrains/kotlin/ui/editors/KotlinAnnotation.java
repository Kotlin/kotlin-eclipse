package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;

public class KotlinAnnotation extends Annotation {

    private final Position position;
    
    public KotlinAnnotation(Position position, String annotationType) {
        super(annotationType, false, "");
        this.position = position;
    }
    
    public KotlinAnnotation(int offset, int length, String annotationType) {
        this(new Position(offset, length), annotationType);
    }
    
    public Position getPosition() {
        return position;
    }
}