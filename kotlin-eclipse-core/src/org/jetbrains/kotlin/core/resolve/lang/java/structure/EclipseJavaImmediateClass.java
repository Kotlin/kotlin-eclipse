package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifier;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeSubstitutor;

import com.google.common.collect.Lists;


public class EclipseJavaImmediateClass implements JavaClassifierType {

    private final JavaClass javaClass; 
    private final JavaTypeSubstitutor substitutor;
    
    public EclipseJavaImmediateClass(JavaClass javaClass, JavaTypeSubstitutor substitutor) {
        this.javaClass = javaClass;
        this.substitutor = substitutor;
    }
    
    @Override
    @NotNull
    public JavaArrayType createArrayType() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public JavaClassifier getClassifier() {
        return javaClass;
    }

    @Override
    @NotNull
    public JavaTypeSubstitutor getSubstitutor() {
        return substitutor;
    }

    @Override
    @NotNull
    public Collection<JavaClassifierType> getSupertypes() {
        return javaClass.getSupertypes();
    }

    @Override
    @NotNull
    public String getPresentableText() {
        return javaClass.getName().asString();
    }

    @Override
    public boolean isRaw() {
        return javaClass.getTypeParameters().size() > 0 & getTypeArguments().size() == 0;
    }

    @Override
    @NotNull
    public List<JavaType> getTypeArguments() {
        List<JavaType> substitutedParameters = Lists.newArrayList();
        for (JavaTypeParameter typeParameter : javaClass.getTypeParameters()) {
            JavaType substituted = substitutor.substitute(typeParameter);
            if (substituted != null) {
                substitutedParameters.add(substituted);
            }
        }
        
        return substitutedParameters;
    }
}
