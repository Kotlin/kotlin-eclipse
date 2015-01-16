/*******************************************************************************
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType;
import org.jetbrains.kotlin.load.java.structure.JavaField;
import org.jetbrains.kotlin.load.java.structure.JavaMethod;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter;

import com.google.common.collect.Lists;

public class EclipseJavaElementFactory {
    private EclipseJavaElementFactory() {
    }
    
    private interface Factory<Binding, Java> {
        @NotNull
        Java create(@NotNull Binding binding);
    }
    
    private static class Factories {
        private static final Factory<IAnnotationBinding, JavaAnnotation> ANNOTATIONS = new Factory<IAnnotationBinding, JavaAnnotation>() {
            @Override
            @NotNull
            public JavaAnnotation create(@NotNull IAnnotationBinding annotationBinding) {
                return new EclipseJavaAnnotation(annotationBinding);
            }
        };
        
        private static final Factory<ITypeBinding, JavaClassifierType> CLASSIFIER_TYPES = new Factory<ITypeBinding, JavaClassifierType>() {
            @Override
            @NotNull
            public JavaClassifierType create(@NotNull ITypeBinding typeBinding) {
                return new EclipseJavaClassifierType(typeBinding);
            }
        };
        
        private static final Factory<ITypeBinding, JavaType> TYPES = new Factory<ITypeBinding, JavaType>() {
            @Override
            @NotNull
            public JavaType create(@NotNull ITypeBinding typeBinding) {
                return EclipseJavaType.create(typeBinding);
            }
        };
        
        private static final Factory<IMethodBinding, JavaMethod> METHODS = new Factory<IMethodBinding, JavaMethod>() {
            @Override
            @NotNull
            public JavaMethod create(@NotNull IMethodBinding methodBinding) {
                return new EclipseJavaMethod(methodBinding);
            }
        };
        
        private static final Factory<IVariableBinding, JavaField> FIELDS = new Factory<IVariableBinding, JavaField>() {
            @Override
            @NotNull
            public JavaField create(@NotNull IVariableBinding variableBinding) {
                return new EclipseJavaField(variableBinding);
            }
        };
        
        private static final Factory<ITypeBinding, JavaTypeParameter> TYPE_PARAMETERS = new Factory<ITypeBinding, JavaTypeParameter>() {
            @Override
            @NotNull
            public JavaTypeParameter create(@NotNull ITypeBinding typeParameterBinding) {
                return new EclipseJavaTypeParameter(typeParameterBinding);
            }
        };
    }
    
    @NotNull
    private static <Binding, Java> List<Java> convert(@NotNull Binding[] elements, @NotNull Factory<Binding, Java> factory) {
        if (elements.length == 0) return Collections.emptyList();
        List<Java> result = Lists.newArrayList();
        for (Binding element : elements) {
            result.add(factory.create(element));
        }
        return result;
    }
    
    @NotNull
    public static List<JavaAnnotation> annotations(@NotNull IAnnotationBinding[] annotations) {
        return convert(annotations, Factories.ANNOTATIONS);
    }
    
    @NotNull
    public static List<JavaClassifierType> classifierTypes(@NotNull ITypeBinding[] classTypes) {
        return convert(classTypes, Factories.CLASSIFIER_TYPES);
    }
    
    @NotNull
    public static List<JavaMethod> methods(@NotNull IMethodBinding[] methods) {
        return convert(methods, Factories.METHODS);
    }
    
    @NotNull
    public static List<JavaField> fields(@NotNull IVariableBinding[] variables) {
        return convert(variables, Factories.FIELDS);
    }
    
    @NotNull
    public static List<JavaType> types(@NotNull ITypeBinding[] types) {
        return convert(types, Factories.TYPES);
    }
    
    @NotNull
    public static List<JavaTypeParameter> typeParameters(@NotNull ITypeBinding[] typeParameters) {
        return convert(typeParameters, Factories.TYPE_PARAMETERS);
    }
}
