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

import org.eclipse.jdt.core.dom.ITypeBinding
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.classifierTypes
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementFactory.typeParameters
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaConstructor
import org.jetbrains.kotlin.load.java.structure.JavaField
import org.jetbrains.kotlin.load.java.structure.JavaMethod
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import java.lang.reflect.Modifier

public class EclipseJavaClass(javaElement: ITypeBinding) : EclipseJavaClassifier<ITypeBinding>(javaElement), JavaClass {
    override val name: Name = SpecialNames.safeIdentifier(binding.getName())
    
    override val isAbstract: Boolean = Modifier.isAbstract(binding.getModifiers())
    
    override val isStatic: Boolean = Modifier.isStatic(binding.getModifiers())
    
    override val isFinal: Boolean = Modifier.isFinal(binding.getModifiers())
    
    override val visibility: Visibility = EclipseJavaElementUtil.getVisibility(binding)
    
    override val typeParameters: List<JavaTypeParameter>
        get() = typeParameters(binding.getTypeParameters())
    
    override val innerClassNames: Collection<Name>
        get() = binding.declaredTypes.mapNotNull { it.name?.takeIf(Name::isValidIdentifier)?.let(Name::identifier) }
    
    override fun findInnerClass(name: Name): JavaClass? {
        return binding.declaredTypes.find { it.name == name.asString() }?.let(::EclipseJavaClass)
    }
    
    override val fqName: FqName? = binding.getQualifiedName()?.let { FqName(it) }
    
    override val isInterface: Boolean = binding.isInterface()
    
    override val isAnnotationType: Boolean = binding.isAnnotation()
    
    override val isEnum: Boolean = binding.isEnum()
    
    override val outerClass: JavaClass? 
        get() = binding.getDeclaringClass()?.let { EclipseJavaClass(it) }
    
    override val supertypes: Collection<JavaClassifierType> 
        get() = classifierTypes(EclipseJavaElementUtil.getSuperTypesWithObject(binding))
    
    override val methods: Collection<JavaMethod> 
        get() = binding.declaredMethods.filterNot { it.isConstructor() }.map(::EclipseJavaMethod)
    
    override val fields: Collection<JavaField>
        get() = binding.getDeclaredFields()
                .filter { 
                    val name = it.getName()
                    name != null && Name.isValidIdentifier(name)
                }
                .map { EclipseJavaField(it) }
    
    override val constructors: Collection<JavaConstructor> 
        get() = binding.declaredMethods.filter { it.isConstructor() }.map(::EclipseJavaConstructor)
    
    override val isDeprecatedInJavaDoc: Boolean = binding.isDeprecated
    
    override val annotations: Collection<JavaAnnotation> 
        get() = binding.annotations.map(::EclipseJavaAnnotation)
    
    override val lightClassOriginKind: LightClassOriginKind?
        get() = binding.javaElement.let {
            if (EclipseJavaElementUtil.isKotlinLightClass(it))
                LightClassOriginKind.SOURCE
            else
                null
        }
}