/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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
*
*******************************************************************************/
package org.jetbrains.kotlin.core.resolve.lang.java.resolver

import javax.inject.Inject
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace

class EclipseTraceBasedJavaResolverCache : JavaResolverCache {
    private lateinit var trace: BindingTrace
    
    @Inject
    fun setTrace(trace: BindingTrace) {
        this.trace = trace
    }

    override fun getClassResolvedFromSource(fqName: FqName): ClassDescriptor? {
        return trace[BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, fqName.toUnsafe()]
    }

    override fun recordMethod(method: JavaMethod, descriptor: SimpleFunctionDescriptor) {
    }

    override fun recordConstructor(element: JavaElement, descriptor: ConstructorDescriptor) {
    }

    override fun recordField(field: JavaField, descriptor: PropertyDescriptor) {
    }

    override fun recordClass(javaClass: JavaClass, descriptor: ClassDescriptor) {
    }
}