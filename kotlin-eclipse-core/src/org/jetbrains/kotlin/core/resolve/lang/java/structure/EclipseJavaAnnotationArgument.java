package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.name.Name;

public abstract class EclipseJavaAnnotationArgument<T extends IBinding> extends EclipseJavaElement<T> implements JavaAnnotationArgument {

    protected EclipseJavaAnnotationArgument(T javaElement) {
        super(javaElement);
    }
    
    @NotNull
    static JavaAnnotationArgument create(@NotNull Object value, @NotNull Name name, @NotNull IJavaProject javaProject) {
        if (value instanceof IAnnotationBinding) {
            return new EclipseJavaAnnotationAsAnnotationArgument((IAnnotationBinding) value, name);
        } else if (value instanceof Object[]) {
            return new EclipseJavaArrayAnnotationArgument((Object[]) value, name, javaProject);
        } else if (value instanceof Class) {
            return new EclipseJavaClassObjectAnnotationArgument((Class<?>) value, name, javaProject);
        } else if (value instanceof IVariableBinding) {
            return new EclipseJavaReferenceAnnotationArgument((IVariableBinding) value);
        } else if (value instanceof String) {
            return new EclipseJavaLiteralAnnotationArgument(value, name);
        }

        throw new IllegalArgumentException("Wrong annotation argument: " + value);
    }
    
    @Override
    @Nullable
    public Name getName() {
        return Name.identifier(getBinding().getName());
    }
}
