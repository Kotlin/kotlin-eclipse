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

import org.eclipse.jdt.core.dom.ITypeBinding
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import kotlin.platform.platformStatic
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.FqName

public abstract class EclipseJavaClassifier<T : ITypeBinding>(javaType: T) : 
		EclipseJavaElement<T>(javaType), JavaClassifier, JavaAnnotationOwner {
	companion object {
		@platformStatic 
		fun create(element: ITypeBinding): JavaClassifier {
			return when {
				element.isTypeVariable() -> EclipseJavaTypeParameter(element)
				element.isClass(), element.isParameterizedType(), 
					element.isInterface(), element.isEnum() -> EclipseJavaClass(element)
				else -> throw IllegalArgumentException("Element: ${element.getName()} is not JavaClassifier")
			}
		}
	}
    
    override public fun getAnnotations(): Collection<JavaAnnotation> {
        return getBinding().getAnnotations().map { EclipseJavaAnnotation(it) }
    }

    override public fun findAnnotation(fqName: FqName): JavaAnnotation? {
        return EclipseJavaElementUtil.findAnnotation(getBinding().getAnnotations(), fqName)
    }
	
	override fun isDeprecatedInJavaDoc(): Boolean =
			getBinding().isDeprecated()
}