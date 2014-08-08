package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.IVariableBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;

public class EclipseJavaField extends EclipseJavaMember<IVariableBinding> implements JavaField {

    protected EclipseJavaField(IVariableBinding javaField) {
        super(javaField);
    }

    @Override
    public boolean isEnumEntry() {
        return getBinding().isEnumConstant();
    }

    @Override
    @NotNull
    public JavaType getType() {
        return EclipseJavaType.create(getBinding().getType());
    }

    @Override
    @NotNull
    public JavaClass getContainingClass() {
        return new EclipseJavaClass(getBinding().getDeclaringClass());
    }
}
