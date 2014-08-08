package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaLiteralAnnotationArgument;
import org.jetbrains.jet.lang.resolve.name.Name;

public class EclipseJavaLiteralAnnotationArgument implements JavaLiteralAnnotationArgument {

    private final Object value;
    private final Name name;

    public EclipseJavaLiteralAnnotationArgument(@NotNull Object value, @NotNull Name name) {
        this.value = value;
        this.name = name;
    }

    @Override
    @Nullable
    public Name getName() {
        return name;
    }

    @Override
    @Nullable
    public Object getValue() {
        return value;
    }

}
