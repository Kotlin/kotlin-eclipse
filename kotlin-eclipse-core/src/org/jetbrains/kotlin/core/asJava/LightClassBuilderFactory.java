package org.jetbrains.kotlin.core.asJava;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import kotlin.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AbstractClassBuilder;
import org.jetbrains.kotlin.codegen.ClassBuilder;
import org.jetbrains.kotlin.codegen.ClassBuilderFactory;
import org.jetbrains.kotlin.codegen.ClassBuilderMode;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;

public class LightClassBuilderFactory implements ClassBuilderFactory {
    public static final Key<Set<Pair<String, String>>> JVM_SIGNATURE = Key.create("JVM_SIGNATURE");

    @Override
    @NotNull
    public ClassBuilderMode getClassBuilderMode() {
        return ClassBuilderMode.LIGHT_CLASSES;
    }

    @Override
    @NotNull
    public ClassBuilder newClassBuilder(@NotNull JvmDeclarationOrigin origin) {
        return new AbstractClassBuilder.Concrete(new BinaryClassWriter()) {
            @Override
            @NotNull
            public MethodVisitor newMethod(@NotNull JvmDeclarationOrigin origin, int access, @NotNull String name,
                    @NotNull String desc, @Nullable String signature, @Nullable String[] exceptions) {
                saveJvmSignature(origin, name, desc);
                return super.newMethod(origin, access, name, desc, signature, exceptions);
            }

            @Override
            @NotNull
            public FieldVisitor newField(@NotNull JvmDeclarationOrigin origin, int access, @NotNull String name,
                    @NotNull String desc, @Nullable String signature, @Nullable Object value) {
                saveJvmSignature(origin, name, desc);
                return super.newField(origin, access, name, desc, signature, value);
            }
            
            private void saveJvmSignature(@NotNull JvmDeclarationOrigin origin, @NotNull String name, @NotNull String desc) {
                PsiElement element = origin.getElement();
                if (element != null) {
                    Set<Pair<String, String>> userData = element.getUserData(JVM_SIGNATURE);
                    if (userData == null) {
                        userData = Collections.newSetFromMap(new ConcurrentHashMap<>());
                        element.putUserData(JVM_SIGNATURE, userData);
                    }
                    userData.add(new Pair<>(desc, name));
                }
            }
        };
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

    @Override
    public void close() {
    }
}
