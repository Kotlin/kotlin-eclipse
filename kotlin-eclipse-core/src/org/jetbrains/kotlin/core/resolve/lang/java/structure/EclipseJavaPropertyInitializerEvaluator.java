package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPropertyInitializerEvaluator;

public class EclipseJavaPropertyInitializerEvaluator implements JavaPropertyInitializerEvaluator {

    @Override
    @Nullable
    public CompileTimeConstant<?> getInitializerConstant(@NotNull JavaField field,
            @NotNull PropertyDescriptor descriptor) {
        // TODO Auto-generated method stub
        return null;
    }

}
