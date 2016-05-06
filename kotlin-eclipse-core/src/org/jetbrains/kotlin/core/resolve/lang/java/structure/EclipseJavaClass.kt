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
package org.jetbrains.kotlin.core.resolve.lang.java.structure

import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.annotations
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.classifierTypes
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.fields
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.methods
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.typeParameters
import java.lang.reflect.Modifier
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.dom.IMethodBinding
import org.eclipse.jdt.core.dom.ITypeBinding
import org.eclipse.jdt.internal.core.BinaryType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaConstructor
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import com.google.common.collect.Lists
import org.jetbrains.kotlin.name.SpecialNames

public class EclipseJavaClass(javaElement: ITypeBinding) : EclipseJavaClassifier<ITypeBinding>(javaElement), JavaClass {
    override val name: Name = SpecialNames.safeIdentifier(getBinding().getName())
    
    override val isAbstract: Boolean = Modifier.isAbstract(getBinding().getModifiers())
    
    override val isStatic: Boolean = Modifier.isStatic(getBinding().getModifiers())
    
    override val isFinal: Boolean = Modifier.isFinal(getBinding().getModifiers())
    
    override val visibility: Visibility = EclipseJavaElementUtil.getVisibility(getBinding())
    
    override val typeParameters: List<JavaTypeParameter>
        get() = typeParameters(getBinding().getTypeParameters())
    
    override val innerClasses: Collection<JavaClass>
        get() = getBinding().declaredTypes.map(::EclipseJavaClass)
    
    override val fqName: FqName? = getBinding().getQualifiedName()?.let { FqName(it) }
    
    override val isInterface: Boolean = getBinding().isInterface()
    
    override val isAnnotationType: Boolean = getBinding().isAnnotation()
    
    override val isEnum: Boolean = getBinding().isEnum()
    
    override val outerClass: JavaClass? 
        get() = getBinding().getDeclaringClass()?.let { EclipseJavaClass(it) }
    
    override val supertypes: Collection<JavaClassifierType> 
        get() = classifierTypes(EclipseJavaElementUtil.getSuperTypesWithObject(getBinding()))
    
    override val methods: Collection<JavaMethod> 
        get() = getBinding().declaredMethods.filterNot { it.isConstructor() }.map(::EclipseJavaMethod)
    
    override val fields: Collection<JavaField>
        get() = getBinding().getDeclaredFields()
                .filter { 
                    val name = it.getName()
                    name != null && Name.isValidIdentifier(name)
                }
                .map { EclipseJavaField(it) }
    
    override val constructors: Collection<JavaConstructor> 
        get() = getBinding().declaredMethods.filter { it.isConstructor() }.map(::EclipseJavaConstructor)
    
    override val isDeprecatedInJavaDoc: Boolean = getBinding().isDeprecated
    
    override val annotations: Collection<JavaAnnotation> 
        get() = getBinding().annotations.map(::EclipseJavaAnnotation)
    
    override val isKotlinLightClass: Boolean
        get() = getBinding().javaElement.let { EclipseJavaElementUtil.isKotlinLightClass(it) }
}