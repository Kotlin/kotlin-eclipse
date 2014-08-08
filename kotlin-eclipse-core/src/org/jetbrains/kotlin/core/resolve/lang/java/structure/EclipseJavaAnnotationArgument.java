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

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.name.Name;

public abstract class EclipseJavaAnnotationArgument<T extends IBinding> extends EclipseJavaElement<T> implements JavaAnnotationArgument {

    protected EclipseJavaAnnotationArgument(T javaElement) {
        super(javaElement);
    }
    
    @NotNull
    static JavaAnnotationArgument create(@NotNull Object value, @NotNull Name name, @NotNull IJavaProject javaProject) {
        if (value instanceof IAnnotationBinding) {
            return new EclipseJavaAnnotationAsAnnotationArgument((IAnnotationBinding) value, name);
        } else if (value instanceof Object[]) {
            return new EclipseJavaArrayAnnotationArgument((Object[]) value, name, javaProject);
        } else if (value instanceof Class) {
            return new EclipseJavaClassObjectAnnotationArgument((Class<?>) value, name, javaProject);
        } else if (value instanceof IVariableBinding) {
            return new EclipseJavaReferenceAnnotationArgument((IVariableBinding) value);
        } else if (value instanceof String) {
            return new EclipseJavaLiteralAnnotationArgument(value, name);
        }

        throw new IllegalArgumentException("Wrong annotation argument: " + value);
    }
    
    @Override
    @Nullable
    public Name getName() {
        return Name.identifier(getBinding().getName());
    }
}
