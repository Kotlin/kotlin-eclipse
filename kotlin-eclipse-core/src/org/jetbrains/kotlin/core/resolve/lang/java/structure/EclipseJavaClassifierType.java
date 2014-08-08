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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifier;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeSubstitutor;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaTypeSubstitutorImpl;

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
    public JavaTypeSubstitutor getSubstitutor() {
        JavaClassifier resolvedType = getClassifier();
        if (resolvedType instanceof JavaClass && getBinding().isParameterizedType()) {
            JavaClass javaClass = (JavaClass) resolvedType;
            List<JavaType> substitutedTypeArguments = getTypeArguments();
            
            int i = 0;
            Map<JavaTypeParameter, JavaType> substitutionMap = new HashMap<JavaTypeParameter, JavaType>();
            boolean isThisRawType = isRaw();
            for (JavaTypeParameter typeParameter : javaClass.getTypeParameters()) {
                substitutionMap.put(typeParameter, !isThisRawType ? substitutedTypeArguments.get(i) : null);
                i++;
            }
            
            return new JavaTypeSubstitutorImpl(substitutionMap);
        }
        
        return JavaTypeSubstitutor.EMPTY;
    }
    
    @Override
    @NotNull
    public Collection<JavaClassifierType> getSupertypes() {
        return EclipseJavaElementUtil.getSuperTypes(getBinding());
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
        List<JavaType> typeArguments = new ArrayList<JavaType>();
        for (ITypeBinding typeArgument : getBinding().getTypeArguments()) {
            typeArguments.add(EclipseJavaType.create(typeArgument));
        }
        
        return typeArguments;
    }

}
