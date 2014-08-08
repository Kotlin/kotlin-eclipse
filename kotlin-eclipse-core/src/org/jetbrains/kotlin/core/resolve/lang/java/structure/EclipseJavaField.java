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

import org.eclipse.jdt.core.dom.IVariableBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaType;

public class EclipseJavaField extends EclipseJavaMember<IVariableBinding> implements JavaField {

    protected EclipseJavaField(IVariableBinding javaField) {
        super(javaField);
    }

    @Override
    public boolean isEnumEntry() {
        return getBinding().isEnumConstant();
    }

    @Override
    @NotNull
    public JavaType getType() {
        return EclipseJavaType.create(getBinding().getType());
    }

    @Override
    @NotNull
    public JavaClass getContainingClass() {
        return new EclipseJavaClass(getBinding().getDeclaringClass());
    }
}
