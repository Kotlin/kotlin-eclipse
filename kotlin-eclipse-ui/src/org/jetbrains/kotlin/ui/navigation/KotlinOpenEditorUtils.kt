package org.jetbrains.kotlin.ui.navigation

import org.jetbrains.kotlin.psi.JetVisitorVoid
import com.intellij.psi.PsiElement
import org.eclipse.jdt.core.IJavaElement
import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.jdt.core.IType
import org.jetbrains.kotlin.psi.JetVisitor
import org.jetbrains.kotlin.psi.JetClass
import org.eclipse.jdt.core.IMethod
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetFile
import kotlin.MutableList
import java.util.ArrayList
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.name.FqName
import org.eclipse.jdt.core.IField
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.core.asJava.LightClassBuilderFactory
import org.jetbrains.kotlin.psi.JetPropertyAccessor
import org.jetbrains.kotlin.psi.JetSecondaryConstructor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.eclipse.jdt.core.IMember
import org.jetbrains.kotlin.psi.JetDeclaration

fun findKotlinDeclaration(element: IJavaElement, jetFile: JetFile): JetElement? {
	val result = ArrayList<JetElement>()
	val visitor = makeVisitor(element, result)
	if (visitor != null) {
		jetFile.acceptChildren(visitor)
	}

	return if (result.size == 1) result[0] else null
}

fun makeVisitor(element: IJavaElement, result: MutableList<JetElement>): JetVisitorVoid? {
	return when (element) {
		is IType -> object : JetAllVisitor() {
			override fun visitClass(jetClass: JetClass){
				visitClassOrObject(jetClass)
			}
			
			override fun visitObjectDeclaration(declaration: JetObjectDeclaration) {
				visitClassOrObject(declaration)
			}
			
			fun visitClassOrObject(jetClassOrObject: JetClassOrObject) {
				if (equalsFqNames(jetClassOrObject, element)) {
					result.add(jetClassOrObject)
					return
				}
				
				jetClassOrObject.acceptChildren(this)
			}
		}
		is IField -> object: JetAllVisitor() {
			override fun visitObjectDeclaration(declaration: JetObjectDeclaration) {
				val objectName = declaration.getName()
				if (objectName != null && objectName == element.getElementName()) {
					if (equalsDeclaringTypes(declaration, element)) {
						result.add(declaration)
						return
					}
				}

				declaration.acceptChildren(this)
			}
		}
		is IMethod -> 
			object : JetAllVisitor() {
				override fun visitNamedFunction(function: JetNamedFunction) {
					visitPropertyOrFunction(function)
				}
			
				override fun visitProperty(property: JetProperty) {
					visitPropertyOrFunction(property)
				}
			
				override fun visitPropertyAccessor(accessor: JetPropertyAccessor) {
					visitPropertyOrFunction(accessor)
				}
			
//				Check primary constructors 
				override fun visitClass(jetClass: JetClass) {
					if (element.isConstructor()) {
						if (equalsJvmSignature(jetClass, element, false) && equalsFqNames(jetClass, element.getDeclaringType())) {
							result.add(jetClass)
							return
						}
					}
					
					jetClass.acceptChildren(this)
				}
			
				override fun visitSecondaryConstructor(constructor: JetSecondaryConstructor) {
					if (element.isConstructor()) {
						if (equalsJvmSignature(constructor, element, false) && equalsDeclaringTypes(constructor, element)) {
							result.add(constructor)
							return
						}
					}
				
					constructor.acceptChildren(this)
				}
			
				fun visitPropertyOrFunction(declaration: JetDeclaration) {
					if (equalsJvmSignature(declaration, element) && equalsDeclaringTypes(declaration, element)) {
						result.add(declaration)
						return
					}
				
					declaration.acceptChildren(this)
				}
			}
		else -> null
	}
}

fun checkFqName(fqName: FqName, javaClass: IType): Boolean {
	val javaFqName = FqName(javaClass.getFullyQualifiedName('.'))
	return fqName == javaFqName
}

fun equalsFqNames(jetClass: JetClassOrObject, javaClass: IType): Boolean {
	val fqName = jetClass.getFqName()
	return if (fqName != null) checkFqName(fqName, javaClass) else false
}

fun equalsJvmSignature(jetElement: JetElement, javaMethod: IMethod, checkPlatformName: Boolean = true): Boolean {
	val jetSignatures = jetElement.getUserData(LightClassBuilderFactory.JVM_SIGNATURE)
	if (jetSignatures == null) return false
	
	val methodSignature = javaMethod.getSignature()
	
	return jetSignatures.any { 
		if (it.first == methodSignature) {
			return when {
				checkPlatformName -> it.second == javaMethod.getElementName()
				else -> true 
			}
		}
		
		false
	}
}

fun equalsDeclaringTypes(jetElement: JetElement, javaMember: IMember): Boolean  {
	val parent = PsiTreeUtil.getParentOfType(
			jetElement, 
			javaClass<JetClassOrObject>(), 
			javaClass<JetObjectDeclaration>(),
			javaClass<JetClass>(),
			javaClass<JetFile>())
	
	val jetFqName = when (parent) {
		is JetClassOrObject -> parent.getFqName()
		is JetFile -> PackageClassUtils.getPackageClassFqName(parent.getPackageFqName())
		else -> null
	}
	
	return if (jetFqName != null) checkFqName(jetFqName, javaMember.getDeclaringType()) else false
}

open class JetAllVisitor() : JetVisitorVoid() {
	override fun visitElement(element: PsiElement) {
		element.acceptChildren(this)
	}
}