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
package org.jetbrains.kotlin.core.resolve.lang.java.resolver;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaResolverCache;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.core.resolve.EclipseBindingContext;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaClass;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaField;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaMethod;

public class EclipseTraceBasedJavaResolverCache implements JavaResolverCache {
    private BindingTrace trace;
    
    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Nullable
    @Override
    public ClassDescriptor getClassResolvedFromSource(@NotNull FqName fqName) {
        return trace.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, fqName.toUnsafe());
    }

    @Nullable
    @Override
    public ClassDescriptor getClass(@NotNull JavaClass javaClass) {
        return trace.get(EclipseBindingContext.ECLIPSE_CLASS, ((EclipseJavaClass) javaClass).getBinding());
    }

    @Override
    public void recordMethod(@NotNull JavaMethod method, @NotNull SimpleFunctionDescriptor descriptor) {
        trace.record(EclipseBindingContext.ECLIPSE_FUNCTION, ((EclipseJavaMethod) method).getBinding(), descriptor);
    }

    @Override
    public void recordConstructor(@NotNull JavaElement element, @NotNull ConstructorDescriptor descriptor) {
        trace.record(EclipseBindingContext.ECLIPSE_CONSTRUCTOR, ((EclipseJavaElement<?>) element).getBinding(), descriptor);
    }

    @Override
    public void recordField(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor) {
        trace.record(EclipseBindingContext.ECLIPSE_VARIABLE, ((EclipseJavaField) field).getBinding(), descriptor);
    }

    @Override
    public void recordClass(@NotNull JavaClass javaClass, @NotNull ClassDescriptor descriptor) {
        trace.record(EclipseBindingContext.ECLIPSE_CLASS, ((EclipseJavaClass) javaClass).getBinding(), descriptor);
    }
}
