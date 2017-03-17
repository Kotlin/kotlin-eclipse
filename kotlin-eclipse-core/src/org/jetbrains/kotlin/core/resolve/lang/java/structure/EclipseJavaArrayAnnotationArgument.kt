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

import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaArrayAnnotationArgument
import org.jetbrains.kotlin.name.Name

public class EclipseJavaArrayAnnotationArgument(
		private val arguments: Array<*>, 
		override val name: Name, 
		private val javaProject: IJavaProject) : JavaArrayAnnotationArgument {
	override public fun getElements(): List<JavaAnnotationArgument> {
		return arguments.map { EclipseJavaAnnotationArgument.create(it!!, name, javaProject) }
	}
}