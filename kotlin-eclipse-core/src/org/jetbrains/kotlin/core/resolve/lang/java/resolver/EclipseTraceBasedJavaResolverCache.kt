/*******************************************************************************
* Copyright 2000-2014 JetBrains s.r.o.
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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.core.resolve.EclipseBindingContext
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaClass
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElement
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaField
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaMethod
import kotlin.properties.Delegates
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.load.java.structure.impl.JavaElementImpl
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaClassifier
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.core.utils.tail

public class EclipseTraceBasedJavaResolverCache : JavaResolverCache {
    private var resolveSession by Delegates.notNull<ResolveSession>()

    private val trace: BindingTrace get() = resolveSession.getTrace()

    Inject
    public fun setSession(resolveSession: ResolveSession) {
        this.resolveSession = resolveSession
    }

    override fun getClassResolvedFromSource(fqName: FqName): ClassDescriptor? {
        return trace.get(FQNAME_TO_CLASS_DESCRIPTOR, fqName.toUnsafe()) ?: findInPackageFragments(fqName)
    }

    override fun recordMethod(method: JavaMethod, descriptor: SimpleFunctionDescriptor) {
        trace.record(EclipseBindingContext.ECLIPSE_FUNCTION, (method as EclipseJavaMethod).getBinding(), descriptor)
    }

    override fun recordConstructor(element: JavaElement, descriptor: ConstructorDescriptor) {
        trace.record(EclipseBindingContext.ECLIPSE_CONSTRUCTOR, (element as EclipseJavaElement<*>).getBinding(), descriptor)
    }

    override fun recordField(field: JavaField, descriptor: PropertyDescriptor) {
        trace.record(EclipseBindingContext.ECLIPSE_VARIABLE, (field as EclipseJavaField).getBinding(), descriptor)
    }

    override fun recordClass(javaClass: JavaClass, descriptor: ClassDescriptor) {
        trace.record(EclipseBindingContext.ECLIPSE_CLASS, (javaClass as EclipseJavaClassifier<*>).getBinding(), descriptor)
    }

    private fun findInPackageFragments(fullFqName: FqName): ClassDescriptor? {
        var fqName = if (fullFqName.isRoot()) fullFqName else fullFqName.parent()

        while (true) {
            val packageDescriptor = resolveSession.getPackageFragment(fqName)
            if (packageDescriptor == null) break

            val result = ResolveSessionUtils.findByQualifiedName(packageDescriptor.getMemberScope(), fullFqName.tail(fqName))
            if (result != null) return result

            if (fqName.isRoot()) break
            fqName = fqName.parent()
        }

        return null
    }
}
