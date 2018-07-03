package org.jetbrains.kotlin.core.resolve.lang.java.structure

import org.eclipse.jdt.core.dom.ITypeBinding
import org.jetbrains.kotlin.load.java.structure.JavaClassObjectAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.name.Name

public class EclipseJavaTypeAsAnnotationArgument(binding: ITypeBinding, override val name: Name?)
	: EclipseJavaAnnotationArgument<ITypeBinding>(binding), JavaClassObjectAnnotationArgument {

	override fun getReferencedType(): JavaType {
		return EclipseJavaType(binding)
	}
}
