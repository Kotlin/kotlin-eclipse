package org.jetbrains.kotlin.core.resolve.lang.java.structure

import org.eclipse.jdt.core.IType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.core.resolve.lang.java.EclipseJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.*

class EclipseOptimizedJavaClass(val eclipseClass: IType) : JavaClass {
    override val name: Name
        get() = SpecialNames.safeIdentifier(eclipseClass.getElementName())
    
    override val constructors: Collection<JavaConstructor>
        get() = throw UnsupportedOperationException()
    
    override val fields: Collection<JavaField>
        get() = throw UnsupportedOperationException()
    
    override val fqName: FqName?
        get() = throw UnsupportedOperationException()
    
    override val innerClassNames: Collection<Name>
        get() = throw UnsupportedOperationException()
    
    override fun findInnerClass(name: Name): JavaClass? {
        throw UnsupportedOperationException()
    }
    
    override val isAnnotationType: Boolean
        get() = throw UnsupportedOperationException()
    
    override val isEnum: Boolean
        get() = throw UnsupportedOperationException()
    
    override val isInterface: Boolean
        get() = throw UnsupportedOperationException()

    override val isRecord: Boolean
        get() = throw UnsupportedOperationException()

    override val isSealed: Boolean
        get() = throw UnsupportedOperationException()

    override val lightClassOriginKind: LightClassOriginKind?
        get() = if (EclipseJavaElementUtil.isKotlinLightClass(eclipseClass)) LightClassOriginKind.SOURCE else null
    
    override val methods: Collection<JavaMethod>
        get() = throw UnsupportedOperationException()
    
    override val outerClass: JavaClass?
        get() = throw UnsupportedOperationException()

    override val permittedTypes: Collection<JavaClassifierType>
        get() = throw UnsupportedOperationException()

    override val recordComponents: Collection<JavaRecordComponent>
        get() = throw UnsupportedOperationException()

    override val supertypes: Collection<JavaClassifierType>
        get() = throw UnsupportedOperationException()
    
    override val annotations: Collection<JavaAnnotation>
        get() = throw UnsupportedOperationException()
    
    override val isDeprecatedInJavaDoc: Boolean
        get() = throw UnsupportedOperationException()

    override fun findAnnotation(fqName: FqName): JavaAnnotation? {
        throw UnsupportedOperationException()
    }

    override val typeParameters: List<JavaTypeParameter>
        get() = throw UnsupportedOperationException()
    
    override val isAbstract: Boolean
        get() = throw UnsupportedOperationException()
    
    override val isFinal: Boolean
        get() = throw UnsupportedOperationException()

    override val isFromSource: Boolean
        get() = !eclipseClass.isBinary

    override val isStatic: Boolean
        get() = throw UnsupportedOperationException()
    
    override val visibility: Visibility
        get() = throw UnsupportedOperationException()

    override fun hasDefaultConstructor() = throw UnsupportedOperationException()
}