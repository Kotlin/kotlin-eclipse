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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.structure.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.tail
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils
import javax.inject.Inject

class EclipseTraceBasedJavaResolverCache(@Inject val trace: BindingTrace, @Inject val resolveSession: ResolveSession) : JavaResolverCache {
	
    override fun getClassResolvedFromSource(fqName: FqName): ClassDescriptor? {
        return trace[BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, fqName.toUnsafe()] ?: findInPackageFragments(fqName)
    }

    override fun recordMethod(p0: JavaMember, p1: SimpleFunctionDescriptor) {
    }

    override fun recordConstructor(element: JavaElement, descriptor: ConstructorDescriptor) {
    }

    override fun recordField(field: JavaField, descriptor: PropertyDescriptor) {
    }

    override fun recordClass(javaClass: JavaClass, descriptor: ClassDescriptor) {
    }
    
    // Copied from org.jetbrains.kotlin.load.java.components.LazyResolveBasedCache
    private fun findInPackageFragments(fullFqName: FqName): ClassDescriptor? {
        var fqName = if (fullFqName.isRoot) fullFqName else fullFqName.parent()
    
        while (true) {
            val packageDescriptor = resolveSession.getPackageFragment(fqName)
            if (packageDescriptor == null) break
    
            val result = ResolveSessionUtils.findClassByRelativePath(
                    packageDescriptor.getMemberScope(), fullFqName.tail(fqName))
            if (result != null) return result
    
            if (fqName.isRoot) break
            fqName = fqName.parent()
        }
    
        return null
    }
}