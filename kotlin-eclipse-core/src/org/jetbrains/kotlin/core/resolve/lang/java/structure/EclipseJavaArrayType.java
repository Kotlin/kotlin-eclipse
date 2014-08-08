package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;

public class EclipseJavaArrayType extends EclipseJavaType<ITypeBinding> implements JavaArrayType {

    public EclipseJavaArrayType(@NotNull ITypeBinding typeBinding) {
        super(typeBinding);
    }

    @Override
    @NotNull
    public JavaType getComponentType() {
        return EclipseJavaType.create(getBinding().getComponentType());
    }

}
