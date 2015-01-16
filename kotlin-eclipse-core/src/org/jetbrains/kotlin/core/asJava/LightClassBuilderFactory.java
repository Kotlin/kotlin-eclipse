package org.jetbrains.kotlin.core.asJava;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.AbstractClassBuilder;
import org.jetbrains.kotlin.codegen.ClassBuilder;
import org.jetbrains.kotlin.codegen.ClassBuilderFactory;
import org.jetbrains.kotlin.codegen.ClassBuilderMode;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.ClassWriter;

public class LightClassBuilderFactory implements ClassBuilderFactory {

    @Override
    @NotNull
    public ClassBuilderMode getClassBuilderMode() {
        return ClassBuilderMode.LIGHT_CLASSES;
    }

    @Override
    @NotNull
    public ClassBuilder newClassBuilder(@NotNull JvmDeclarationOrigin origin) {
        return new AbstractClassBuilder.Concrete(new BinaryClassWriter());
    }

    @Override
    public String asText(ClassBuilder builder) {
        throw new UnsupportedOperationException("BINARIES generator asked for text");
    }

    @Override
    public byte[] asBytes(ClassBuilder builder) {
        ClassWriter visitor = (ClassWriter) builder.getVisitor();
        return visitor.toByteArray();
    }
    
}