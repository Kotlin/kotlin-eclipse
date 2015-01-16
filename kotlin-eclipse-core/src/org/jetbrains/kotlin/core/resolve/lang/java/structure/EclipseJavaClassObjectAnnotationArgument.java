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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaClassObjectAnnotationArgument;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder;

public class EclipseJavaClassObjectAnnotationArgument implements JavaClassObjectAnnotationArgument {

    private final Class<?> javaClass;
    private final IJavaProject javaProject;
    private final Name name;
    
    protected EclipseJavaClassObjectAnnotationArgument(Class<?> javaClass, @NotNull Name name, @NotNull IJavaProject javaProject) {
        this.javaClass = javaClass;
        this.name = name;
        this.javaProject = javaProject;
    }

    @Override
    @NotNull
    public JavaType getReferencedType() {
        ITypeBinding typeBinding = EclipseJavaClassFinder.findType(new FqName(javaClass.getCanonicalName()), javaProject);
        assert typeBinding != null;
        return EclipseJavaType.create(typeBinding);
    }

    @Override
    @Nullable
    public Name getName() {
        return name;
    }
}
