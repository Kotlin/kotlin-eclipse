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
package org.jetbrains.kotlin.core.resolve.lang.java.structure

import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.core.dom.ITypeBinding
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameterListOwner
import org.jetbrains.kotlin.load.java.structure.JavaTypeProvider
import org.jetbrains.kotlin.name.Name
import com.google.common.collect.Lists

public class EclipseJavaTypeParameter(binding: ITypeBinding) : EclipseJavaClassifier<ITypeBinding>(binding), JavaTypeParameter {
    override public fun getName(): Name = Name.identifier(getBinding().getName())
    
    override public fun getUpperBounds(): Collection<JavaClassifierType> {
        return getBinding().getTypeBounds().map { EclipseJavaClassifierType(it) }
    }
    
    override public fun getOwner(): JavaTypeParameterListOwner? {
        val methodOwner = getBinding().getDeclaringMethod()
        if (methodOwner != null) {
            return if (methodOwner.isConstructor()) EclipseJavaConstructor(methodOwner) else EclipseJavaMethod(methodOwner) 
        }
        
        val typeOwner = getBinding().getDeclaringClass()
        if (typeOwner != null) {
            return EclipseJavaClass(typeOwner)
        }
        
        return null
    }
    
    override public fun getType(): JavaType {
        return EclipseJavaType.create(getBinding().getTypeDeclaration())
    }
    
    override public fun getTypeProvider(): JavaTypeProvider {
        return EclipseJavaTypeProvider(getBinding().getJavaElement().getJavaProject())
    }
}