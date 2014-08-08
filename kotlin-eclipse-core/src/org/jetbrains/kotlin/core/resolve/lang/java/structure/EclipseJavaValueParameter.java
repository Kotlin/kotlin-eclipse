package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public class EclipseJavaValueParameter extends EclipseJavaElement<ITypeBinding> implements JavaValueParameter {

    private final String name;
    private final boolean isVararg;
    private final IAnnotationBinding[] annotationBindings;
    
    public EclipseJavaValueParameter(ITypeBinding type, IAnnotationBinding annotationBindings[], 
            String name, boolean isVararg) {
        super(type);
        this.name = name;
        this.isVararg = isVararg;
        this.annotationBindings = Arrays.copyOf(annotationBindings, annotationBindings.length);
    }

    @Override
    @NotNull
    public Collection<JavaAnnotation> getAnnotations() {
        return EclipseJavaElementUtil.convertAnnotationBindings(annotationBindings);
    }

    @Override
    @Nullable
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return EclipseJavaElementUtil.findAnnotationIn(annotationBindings, fqName);
    }

    @Override
    @Nullable
    public Name getName() {
        return Name.identifier(name);
    }

    @Override
    @NotNull
    public JavaType getType() {
        return EclipseJavaType.create(getBinding());
    }

    @Override
    public boolean isVararg() {
        return isVararg;
    }
}
