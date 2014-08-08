package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPrimitiveType;

public class EclipseJavaPrimitiveType extends EclipseJavaType<ITypeBinding> implements JavaPrimitiveType {

    public EclipseJavaPrimitiveType(ITypeBinding typeBinding) {
        super(typeBinding);
    }

    @Override
    @NotNull
    public String getCanonicalText() {
        return getBinding().getName();
    }
}
