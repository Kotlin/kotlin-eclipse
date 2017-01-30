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
import org.jetbrains.kotlin.name.Name
import com.google.common.collect.Lists
import org.jetbrains.kotlin.name.SpecialNames

public class EclipseJavaTypeParameter(binding: ITypeBinding) : EclipseJavaClassifier<ITypeBinding>(binding), JavaTypeParameter {
    override val name: Name = SpecialNames.safeIdentifier(binding.getName())
    
    override val upperBounds: Collection<JavaClassifierType>
        get() = binding.getTypeBounds().map(::EclipseJavaClassifierType)
}