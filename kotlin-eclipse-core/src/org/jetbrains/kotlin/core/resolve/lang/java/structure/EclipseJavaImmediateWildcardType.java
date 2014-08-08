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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeProvider;
import org.jetbrains.jet.lang.resolve.java.structure.JavaWildcardType;


public class EclipseJavaImmediateWildcardType implements JavaWildcardType {
    
    private final JavaType bound;
    private final boolean isExtends;
    private final JavaTypeProvider typeProvider;
    
    public EclipseJavaImmediateWildcardType(@Nullable JavaType bound, boolean isExtends, 
            @NotNull JavaTypeProvider typeProvider) {
        this.bound = bound;
        this.isExtends = isExtends;
        this.typeProvider = typeProvider;
    }

    @Override
    @NotNull
    public JavaArrayType createArrayType() {
        throw new IllegalStateException("Creating array of wildcard type");
    }

    @Override
    @Nullable
    public JavaType getBound() {
        return bound;
    }

    @Override
    @NotNull
    public JavaTypeProvider getTypeProvider() {
        return typeProvider;
    }

    @Override
    public boolean isExtends() {
        return isExtends;
    }
    
}
