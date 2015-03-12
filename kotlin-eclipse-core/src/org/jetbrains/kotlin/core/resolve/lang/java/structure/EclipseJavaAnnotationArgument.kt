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
package org.jetbrains.kotlin.core.resolve.lang.java.structure

import org.eclipse.jdt.core.dom.IBinding
import org.jetbrains.kotlin.name.Name
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.eclipse.jdt.core.dom.IAnnotationBinding
import org.eclipse.jdt.core.dom.IVariableBinding
	
public abstract class EclipseJavaAnnotationArgument<T : IBinding>(javaElement: T) : 
	EclipseJavaElement<T>(javaElement), JavaAnnotationArgument {
	
	override val name: Name?
		get() = Name.identifier(getBinding().getName())
	
	default object {
		fun create(value: Any, name: Name, javaProject: IJavaProject): JavaAnnotationArgument {
			return when (value) {
				is IAnnotationBinding -> EclipseJavaAnnotationAsAnnotationArgument(value, name)
				is IVariableBinding -> EclipseJavaReferenceAnnotationArgument(value)
				is Array<Any> -> EclipseJavaArrayAnnotationArgument(value, name, javaProject) 
				is Class<*> -> EclipseJavaClassObjectAnnotationArgument(value, name, javaProject)
				is String -> EclipseJavaLiteralAnnotationArgument(value, name)
				else -> throw IllegalArgumentException("Wrong annotation argument: $value")
			}
		}
	}
}