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

import static org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.types;

import java.util.List;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaClassifier;
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType;
import org.jetbrains.kotlin.load.java.structure.JavaType;

public class EclipseJavaClassifierType extends EclipseJavaType<ITypeBinding> implements JavaClassifierType {

    public EclipseJavaClassifierType(ITypeBinding typeBinding) {
        super(typeBinding);
    }

    @Override
    @Nullable
    public JavaClassifier getClassifier() {
        return EclipseJavaClassifier.create(getBinding().getTypeDeclaration());
    }
    
    @Override
    @NotNull
    public String getPresentableText() {
        return getBinding().getQualifiedName();
    }

    @Override
    public boolean isRaw() {
        return getBinding().isRawType();
    }

    @Override
    @NotNull
    public List<JavaType> getTypeArguments() {
        return types(getBinding().getTypeArguments());
    }

    @Override
    @NotNull
    public String getClassifierQualifiedName() {
        return getBinding().getName();
    }
}
