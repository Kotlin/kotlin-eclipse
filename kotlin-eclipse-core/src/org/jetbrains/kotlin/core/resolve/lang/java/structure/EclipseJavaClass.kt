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
import org.jetbrains.kotlin.load.java.structure.JavaTypeSubstitutor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import com.google.common.collect.Lists
import org.jetbrains.kotlin.load.java.structure.JavaClass.OriginKind

public class EclipseJavaClass(javaElement: ITypeBinding) : EclipseJavaClassifier<ITypeBinding>(javaElement), JavaClass {
    override public fun getAnnotations(): Collection<JavaAnnotation> {
        return annotations(getBinding().getAnnotations())
    }
    
    override public fun findAnnotation(fqName: FqName): JavaAnnotation? {
        return EclipseJavaElementUtil.findAnnotation(getBinding().getAnnotations(), fqName)
    }
    
    override public fun getName(): Name = Name.guess(getBinding().getName())
    
    override public fun isAbstract(): Boolean = Modifier.isAbstract(getBinding().getModifiers())
    
    override public fun isStatic(): Boolean = Modifier.isStatic(getBinding().getModifiers())
    
    override public fun isFinal(): Boolean = Modifier.isFinal(getBinding().getModifiers())
    
    override public fun getVisibility(): Visibility = EclipseJavaElementUtil.getVisibility(getBinding())
    
    override public fun getTypeParameters(): List<JavaTypeParameter> {
    	return typeParameters(getBinding().getTypeParameters())
    }
    
    override public fun getInnerClasses(): Collection<JavaClass> {
        return getBinding().getDeclaredTypes().map { EclipseJavaClass(it) }
    }
    
    override public fun getFqName(): FqName? = FqName(getBinding().getQualifiedName())
    
    override public fun isInterface(): Boolean = getBinding().isInterface()
    
    override public fun isAnnotationType(): Boolean = getBinding().isAnnotation()
    
    override public fun isEnum(): Boolean = getBinding().isEnum()
    
    override public fun getOuterClass(): JavaClass? {
        val outerClass = getBinding().getDeclaringClass()
        return if (outerClass != null) EclipseJavaClass(outerClass) else null
    }
    
    override public fun getSupertypes(): Collection<JavaClassifierType> {
        return classifierTypes(EclipseJavaElementUtil.getSuperTypesWithObject(getBinding()))
    }
    
    override public fun getMethods(): Collection<JavaMethod> {
        return getBinding().getDeclaredMethods()
        		.filterNot { it.isConstructor() }
        		.map { EclipseJavaMethod(it) }
    }
    
    override public fun getFields(): Collection<JavaField> {
        return fields(getBinding().getDeclaredFields())
    }
    
    override public fun getConstructors(): Collection<JavaConstructor> {
        return getBinding().getDeclaredMethods()
        		.filter { it.isConstructor() }
        		.map { EclipseJavaConstructor(it) }
    }
    
    override public fun getDefaultType(): JavaClassifierType {
        return EclipseJavaClassifierType(getBinding().getTypeDeclaration())
    }
    
    override public fun getOriginKind(): OriginKind {
        val javaType = getBinding().getJavaElement() as IType
        return when {
            EclipseJavaElementUtil.isKotlinLightClass(javaType) -> OriginKind.KOTLIN_LIGHT_CLASS
            javaType is BinaryType -> OriginKind.COMPILED
            else -> OriginKind.SOURCE 
        }
    }
    
    override public fun createImmediateType(substitutor: JavaTypeSubstitutor): JavaType {
        return EclipseJavaImmediateClass(this, substitutor)
    }
}