package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationAsAnnotationArgument;
import org.jetbrains.jet.lang.resolve.name.Name;

public class EclipseJavaAnnotationAsAnnotationArgument implements JavaAnnotationAsAnnotationArgument {

    private final IAnnotationBinding annotationBinding;
    private final Name name;
    
    protected EclipseJavaAnnotationAsAnnotationArgument(@NotNull IAnnotationBinding annotation, @NotNull Name name) {
        this.annotationBinding = annotation;
        this.name = name;
    }

    @Override
    @NotNull
    public JavaAnnotation getAnnotation() {
        return new EclipseJavaAnnotation(annotationBinding);
    }

    @Override
    @Nullable
    public Name getName() {
        return name;
    }
}
