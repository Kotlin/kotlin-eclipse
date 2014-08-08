package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifier;

public abstract class EclipseJavaClassifier<T extends ITypeBinding> extends EclipseJavaElement<T> implements JavaClassifier {
    public EclipseJavaClassifier(@NotNull T javaType) {
        super(javaType);
    }
    
    static JavaClassifier create(@NotNull ITypeBinding element) {
        if (element.isTypeVariable()) {
            return new EclipseJavaTypeParameter(element);
        } else if (element.isClass() || element.isParameterizedType() || element.isInterface() ||
                element.isEnum()) {
            return new EclipseJavaClass(element);
        } else {
            throw new IllegalArgumentException("Element: " + element.getName() + " is not JavaClassifier");
        }
    }
}
