/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import static org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.annotations;

import java.util.Collection;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner;
import org.jetbrains.kotlin.load.java.structure.JavaArrayType;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.name.FqName;

public class EclipseJavaType<T extends ITypeBinding> implements JavaType, JavaAnnotationOwner {

    private final T binding;
    
    public EclipseJavaType(@NotNull T binding) {
        this.binding = binding;
    }
    
    public static EclipseJavaType<?> create(@NotNull ITypeBinding typeBinding) {
        if (typeBinding.isPrimitive()) {
            return new EclipseJavaPrimitiveType(typeBinding); 
        } else if (typeBinding.isArray()) {
            return new EclipseJavaArrayType(typeBinding);
        } else if (typeBinding.isClass() || typeBinding.isTypeVariable() || 
                typeBinding.isInterface() || typeBinding.isParameterizedType() || 
                typeBinding.isEnum()) {
            return new EclipseJavaClassifierType(typeBinding);
        } else if (typeBinding.isWildcardType()) {
            return new EclipseJavaWildcardType(typeBinding);
        } else {
            throw new UnsupportedOperationException("Unsupported EclipseType: " + typeBinding);
        }
    }
    
    @NotNull
    public T getBinding() {
        return binding;
    }
    
    @Override
    public int hashCode() {
        return getBinding().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EclipseJavaType && getBinding().equals(((EclipseJavaType<?>) obj).getBinding());
    }

    @Override
    @NotNull
    public JavaArrayType createArrayType() {
        return new EclipseJavaArrayType(getBinding().createArrayType(1));
    }
    
    @Override
    @NotNull
    public Collection<JavaAnnotation> getAnnotations() {
        return annotations(getBinding().getAnnotations());
    }

    @Override
    @Nullable
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return EclipseJavaElementUtil.findAnnotation(getBinding().getAnnotations(), fqName);
    }

}
