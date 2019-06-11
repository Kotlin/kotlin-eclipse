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

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.dom.IAnnotationBinding
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import java.util.*

class EclipseJavaAnnotation(javaAnnotation: IAnnotationBinding) :
    EclipseJavaElement<IAnnotationBinding>(javaAnnotation), JavaAnnotation
{
    private val javaProject: IJavaProject = javaAnnotation.annotationType.getPackage().javaElement.javaProject

    override val arguments: Collection<JavaAnnotationArgument>
        get() {
            val arguments = ArrayList<JavaAnnotationArgument>()
            for (memberValuePair in binding.declaredMemberValuePairs) {
                arguments.add(
                    EclipseJavaAnnotationArgument.create(
                        memberValuePair.value,
                        Name.identifier(memberValuePair.name),
                        javaProject
                    )
                )
            }

            return arguments
        }

    override val classId: ClassId?
        get() {
            val annotationType = binding.annotationType
            return if (annotationType != null) EclipseJavaElementUtil.computeClassId(annotationType) else null
        }

    override fun resolve(): JavaClass? {
        val annotationType = binding.annotationType
        return if (annotationType != null) EclipseJavaClass(annotationType) else null
    }
}
