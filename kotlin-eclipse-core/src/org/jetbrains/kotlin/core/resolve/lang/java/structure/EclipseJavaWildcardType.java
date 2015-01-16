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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.JavaTypeProvider;
import org.jetbrains.kotlin.load.java.structure.JavaWildcardType;

public class EclipseJavaWildcardType extends EclipseJavaType<ITypeBinding> implements JavaWildcardType {

    public EclipseJavaWildcardType(@NotNull ITypeBinding typeBinding) {
        super(typeBinding);
    }

    @Override
    @Nullable
    public JavaType getBound() {
        ITypeBinding bound = getBinding().getBound();
        return bound != null ? EclipseJavaType.create(bound) : null;
    }

    @Override
    public boolean isExtends() {
        return getBinding().isUpperbound();
    }

    @Override
    @NotNull
    public JavaTypeProvider getTypeProvider() {
        return new EclipseJavaTypeProvider(getBinding().getJavaElement().getJavaProject());
    }

}
