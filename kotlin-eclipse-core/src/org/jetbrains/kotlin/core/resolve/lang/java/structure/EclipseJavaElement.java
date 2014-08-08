package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.IBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;

public abstract class EclipseJavaElement<T extends IBinding> implements JavaElement {
    
    private final T binding;
    
    protected EclipseJavaElement(@NotNull T binding) {
        this.binding = binding;
    }
    
    @NotNull
    public T getBinding() {
        return binding;
    }
    
    @NotNull
    public IJavaProject getJavaProject() {
        return binding.getJavaElement().getJavaProject();
    }
    
    @Override
    public int hashCode() {
        return getBinding().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EclipseJavaElement && getBinding().equals(((EclipseJavaElement<?>) obj).getBinding());
    }
}
