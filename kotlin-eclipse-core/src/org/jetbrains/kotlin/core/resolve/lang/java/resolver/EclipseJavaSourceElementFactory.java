package org.jetbrains.kotlin.core.resolve.lang.java.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.resolve.java.sources.JavaSourceElementFactory;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;

public class EclipseJavaSourceElementFactory implements JavaSourceElementFactory {

    @Override
    @NotNull
    public SourceElement source(@NotNull JavaElement javaElement) {
        return new EclipseSourceElement(javaElement);
    }

}
