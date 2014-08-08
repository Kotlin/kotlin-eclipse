package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeProvider;
import org.jetbrains.jet.lang.resolve.java.structure.JavaWildcardType;


public class EclipseJavaImmediateWildcardType implements JavaWildcardType {
    
    private final JavaType bound;
    private final boolean isExtends;
    private final JavaTypeProvider typeProvider;
    
    public EclipseJavaImmediateWildcardType(@Nullable JavaType bound, boolean isExtends, 
            @NotNull JavaTypeProvider typeProvider) {
        this.bound = bound;
        this.isExtends = isExtends;
        this.typeProvider = typeProvider;
    }

    @Override
    @NotNull
    public JavaArrayType createArrayType() {
        throw new IllegalStateException("Creating array of wildcard type");
    }

    @Override
    @Nullable
    public JavaType getBound() {
        return bound;
    }

    @Override
    @NotNull
    public JavaTypeProvider getTypeProvider() {
        return typeProvider;
    }

    @Override
    public boolean isExtends() {
        return isExtends;
    }
    
}
