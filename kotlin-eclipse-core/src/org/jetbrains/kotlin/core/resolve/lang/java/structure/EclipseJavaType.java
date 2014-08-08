package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;

public class EclipseJavaType<T extends ITypeBinding> implements JavaType {

    private final T binding;
    
    public EclipseJavaType(@NotNull T binding) {
        this.binding = binding;
    }
    
    public static EclipseJavaType<?> create(@NotNull ITypeBinding typeBinding) {
        if (typeBinding.isPrimitive()) {
            return new EclipseJavaPrimitiveType(typeBinding); 
        } else if (typeBinding.isArray()) {
            return new EclipseJavaArrayType(typeBinding);
        } else if (typeBinding.isClass() || typeBinding.isTypeVariable() || 
                typeBinding.isInterface() || typeBinding.isParameterizedType() || 
                typeBinding.isEnum()) {
            return new EclipseJavaClassifierType(typeBinding);
        } else if (typeBinding.isWildcardType()) {
            return new EclipseJavaWildcardType(typeBinding);
        } else {
            throw new UnsupportedOperationException("Unsupported EclipseType: " + typeBinding);
        }
    }
    
    @NotNull
    public T getBinding() {
        return binding;
    }
    
    @Override
    public int hashCode() {
        return getBinding().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EclipseJavaType && getBinding().equals(((EclipseJavaType<?>) obj).getBinding());
    }

    @Override
    @NotNull
    public JavaArrayType createArrayType() {
        return new EclipseJavaArrayType(getBinding().createArrayType(1));
    }
}
