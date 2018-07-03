package org.jetbrains.kotlin.core.resolve.lang.java.structure

import org.eclipse.jdt.core.dom.ITypeBinding
import org.jetbrains.kotlin.load.java.structure.JavaEnumValueAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.load.java.structure.JavaClassObjectAnnotationArgument

public class EclipseJavaTypeAsAnnotationArgument(binding: ITypeBinding, override val name: Name?)
	: EclipseJavaAnnotationArgument<ITypeBinding>(binding), JavaClassObjectAnnotationArgument {

	override fun getReferencedType(): JavaType {
		return EclipseJavaType(binding)
	}
}
