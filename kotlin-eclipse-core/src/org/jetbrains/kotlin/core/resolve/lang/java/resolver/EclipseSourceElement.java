package org.jetbrains.kotlin.core.resolve.lang.java.resolver;

import org.eclipse.jdt.core.dom.IBinding;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement;

public class EclipseSourceElement implements SourceElement {
    private final JavaElement javaElement;
    
    public EclipseSourceElement(JavaElement javaElement) {
        this.javaElement = javaElement;
    }
    
    public IBinding getBinding() {
        return ((EclipseJavaElement<?>) javaElement).getBinding();
    }
}
