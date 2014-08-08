package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeProvider;
import org.jetbrains.jet.lang.resolve.java.structure.JavaWildcardType;

public class EclipseJavaWildcardType extends EclipseJavaType<ITypeBinding> implements JavaWildcardType {

    public EclipseJavaWildcardType(@NotNull ITypeBinding typeBinding) {
        super(typeBinding);
    }

    @Override
    @Nullable
    public JavaType getBound() {
        ITypeBinding bound = getBinding().getBound();
        return bound != null ? EclipseJavaType.create(bound) : null;
    }

    @Override
    public boolean isExtends() {
        return getBinding().isUpperbound();
    }

    @Override
    @NotNull
    public JavaTypeProvider getTypeProvider() {
        return new EclipseJavaTypeProvider(getBinding().getJavaElement().getJavaProject());
    }

}
