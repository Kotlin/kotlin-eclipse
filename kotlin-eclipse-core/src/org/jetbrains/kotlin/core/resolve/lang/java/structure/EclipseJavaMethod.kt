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
 */
package org.jetbrains.kotlin.core.resolve.lang.java.structure

import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.typeParameters

import org.eclipse.jdt.core.dom.IMethodBinding
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaValueParameter
import org.jetbrains.kotlin.name.Name

class EclipseJavaMethod(method: IMethodBinding) : EclipseJavaMember<IMethodBinding>(method), JavaMethod {

    override val typeParameters: List<JavaTypeParameter>
        get() = typeParameters(binding.typeParameters)

    override val valueParameters: List<JavaValueParameter>
        get() = EclipseJavaElementUtil.getValueParameters(binding)

    override val annotationParameterDefaultValue: JavaAnnotationArgument?
        get() = with(binding) {
            defaultValue?.let {
                EclipseJavaAnnotationArgument.create(defaultValue, Name.identifier(name), javaElement.javaProject)
            }
        }

    override val returnType: JavaType
        get() = EclipseJavaType.create(binding.returnType)

    override val containingClass: JavaClass
        get() = EclipseJavaClass(binding.declaringClass)
}
