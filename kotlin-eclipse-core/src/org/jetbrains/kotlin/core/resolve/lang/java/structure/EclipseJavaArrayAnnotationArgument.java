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

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaArrayAnnotationArgument;
import org.jetbrains.jet.lang.resolve.name.Name;

import com.google.common.collect.Lists;

public class EclipseJavaArrayAnnotationArgument implements JavaArrayAnnotationArgument {

    private final Object[] arguments;
    private final Name name;
    private final IJavaProject javaProject;
    
    protected EclipseJavaArrayAnnotationArgument(@NotNull Object[] arguments, @NotNull Name name,
            @NotNull IJavaProject javaProject) {
        this.arguments = arguments;
        this.name = name;
        this.javaProject = javaProject;
    }

    @Override
    @NotNull
    public List<JavaAnnotationArgument> getElements() {
        List<JavaAnnotationArgument> annotationArguments = Lists.newArrayList();
        for (Object argument : arguments) {
            annotationArguments.add(EclipseJavaAnnotationArgument.create(argument, name, javaProject));
        }
        
        return annotationArguments;
    }

    @Override
    @Nullable
    public Name getName() {
        return name;
    }
}
