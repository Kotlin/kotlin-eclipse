/*******************************************************************************
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.jetbrains.kotlin.core.resolve.lang.java.resolver;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaResolverCache;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaElement;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class EclipseLazyResolveBasedCache implements JavaResolverCache {
    private ResolveSession resolveSession;
    private final EclipseTraceBasedJavaResolverCache traceBasedCache = new EclipseTraceBasedJavaResolverCache();
    
    @Inject
    public void setSession(ResolveSession resolveSession) {
        this.resolveSession = resolveSession;
        traceBasedCache.setTrace(this.resolveSession.getTrace());
    }
    
//    TODO: Add implementation as in Kotlin(LazyResolveBasedCache)
    @Override
    @Nullable
    public ClassDescriptor getClassResolvedFromSource(@NotNull FqName fqName) {
        ClassDescriptor descriptor = traceBasedCache.getClassResolvedFromSource(fqName);
        if (descriptor != null) return descriptor;
        
        return null;
    }

    @Override
    public void recordMethod(@NotNull JavaMethod method, @NotNull SimpleFunctionDescriptor descriptor) {
        traceBasedCache.recordMethod(method, descriptor);
    }

    @Override
    public void recordConstructor(@NotNull JavaElement element, @NotNull ConstructorDescriptor descriptor) {
        traceBasedCache.recordConstructor(element, descriptor);
    }

    @Override
    public void recordField(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor) {
        traceBasedCache.recordField(field, descriptor);
    }

    @Override
    public void recordClass(@NotNull JavaClass javaClass, @NotNull ClassDescriptor descriptor) {
        traceBasedCache.recordClass(javaClass, descriptor);
    }
}
