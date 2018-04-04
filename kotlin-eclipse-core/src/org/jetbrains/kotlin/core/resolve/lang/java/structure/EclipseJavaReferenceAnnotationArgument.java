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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaEnumValueAnnotationArgument;
import org.jetbrains.kotlin.load.java.structure.JavaField;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

public class EclipseJavaReferenceAnnotationArgument extends EclipseJavaAnnotationArgument<IVariableBinding>
        implements JavaEnumValueAnnotationArgument {

    protected EclipseJavaReferenceAnnotationArgument(IVariableBinding javaElement) {
        super(javaElement);
    }

    @Nullable
    public JavaField resolve() {
        return new EclipseJavaField(getBinding());
    }

    @Override
    @Nullable
    public Name getEntryName() {
        return Name.identifier(getBinding().getName());
    }

    @Override
    @Nullable
    public ClassId getEnumClassId() {
        String className = getBinding().getType().getQualifiedName();
        return ClassId.topLevel(new FqName(className));
    }
}
