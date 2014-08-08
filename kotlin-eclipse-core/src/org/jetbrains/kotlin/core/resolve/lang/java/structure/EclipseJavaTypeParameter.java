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

import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClassifierType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameterListOwner;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeProvider;
import org.jetbrains.jet.lang.resolve.name.Name;

import com.google.common.collect.Lists;

public class EclipseJavaTypeParameter extends EclipseJavaClassifier<ITypeBinding> implements JavaTypeParameter {

    protected EclipseJavaTypeParameter(ITypeBinding binding) {
        super(binding);
    }

    @Override
    @NotNull
    public Name getName() {
        return Name.identifier(getBinding().getName());
    }

    @Override
    public int getIndex() {
        JavaTypeParameterListOwner owner = getOwner();
        if (owner == null) {
            return 0;
        }
        
        int typeParameterNum = 0;
        for (JavaTypeParameter ownerParameter : owner.getTypeParameters()) {
            if (ownerParameter.equals(this)) {
                return typeParameterNum;
            }
            typeParameterNum++;
        }
        
        return -1;
    }

    @Override
    @NotNull
    public Collection<JavaClassifierType> getUpperBounds() {
        List<JavaClassifierType> bounds = Lists.newArrayList();
        for (ITypeBinding bound : getBinding().getTypeBounds()) {
            bounds.add(new EclipseJavaClassifierType(bound));
        }
        
        return bounds;
    }

    @Override
    @Nullable
    public JavaTypeParameterListOwner getOwner() {
        IMethodBinding methodOwner = getBinding().getDeclaringMethod();
        if (methodOwner != null) {
            return new EclipseJavaMethod(methodOwner);
        }
        
        ITypeBinding typeOwner = getBinding().getDeclaringClass();
        if (typeOwner != null) {
            return new EclipseJavaClass(typeOwner);
        }
        
        return null;
    }

    @Override
    @NotNull
    public JavaType getType() {
        return EclipseJavaType.create(getBinding().getTypeDeclaration());
    }

    @Override
    @NotNull
    public JavaTypeProvider getTypeProvider() {
        return new EclipseJavaTypeProvider(getBinding().getJavaElement().getJavaProject());
    }

}
