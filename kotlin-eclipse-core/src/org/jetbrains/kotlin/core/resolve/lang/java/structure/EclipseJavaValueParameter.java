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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaValueParameter;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public class EclipseJavaValueParameter extends EclipseJavaElement<ITypeBinding> implements JavaValueParameter {

    private final String name;
    private final boolean isVararg;
    private final IAnnotationBinding[] annotationBindings;
    
    public EclipseJavaValueParameter(ITypeBinding type, IAnnotationBinding annotationBindings[], 
            String name, boolean isVararg) {
        super(type);
        this.name = name;
        this.isVararg = isVararg;
        this.annotationBindings = Arrays.copyOf(annotationBindings, annotationBindings.length);
    }

    @Override
    @NotNull
    public Collection<JavaAnnotation> getAnnotations() {
        return annotations(annotationBindings);
    }

    @Override
    @Nullable
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return EclipseJavaElementUtil.findAnnotation(annotationBindings, fqName);
    }

    @Override
    @Nullable
    public Name getName() {
        return Name.identifier(name);
    }

    @Override
    @NotNull
    public JavaType getType() {
        return EclipseJavaType.create(getBinding());
    }

    @Override
    public boolean isVararg() {
        return isVararg;
    }
}
