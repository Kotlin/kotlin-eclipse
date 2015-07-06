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
import org.jetbrains.kotlin.psi.JetEnumEntry
import org.jetbrains.kotlin.psi.JetPrimaryConstructor
import org.jetbrains.kotlin.psi.JetConstructor

fun findKotlinDeclaration(element: IJavaElement, jetFile: JetFile): JetElement? {
	val result = ArrayList<JetElement>()
	val visitor = makeVisitor(element, result)
	if (visitor != null) {
		jetFile.acceptChildren(visitor)
	}

	return result.firstOrNull()
}

fun makeVisitor(element: IJavaElement, result: MutableList<JetElement>): JetVisitorVoid? {
	return when (element) {
		is IType -> object : JetAllVisitor() {
			override fun visitClassOrObject(jetClassOrObject: JetClassOrObject) {
				if (jetClassOrObject.getFqName() == element.getFqName()) {
					result.add(jetClassOrObject)
					return
				}
				
				jetClassOrObject.acceptChildren(this)
			}
		}
		is IField -> object: JetAllVisitor() {
			override fun visitObjectDeclaration(declaration: JetObjectDeclaration) {
				visitObjectOrEnum(declaration)
				declaration.acceptChildren(this)
			}
			
			override fun visitEnumEntry(enumEntry: JetEnumEntry) {
				visitObjectOrEnum(enumEntry)
			}
			
			fun visitObjectOrEnum(declaration: JetClassOrObject) {
				if (equalsJvmSignature(declaration, element)) {
					result.add(declaration)
				}
			}
		}
		is IMethod -> 
			object : JetAllVisitor() {
				override fun visitNamedFunction(function: JetNamedFunction) {
					visitExplicitDeclaration(function)
				}
			
				override fun visitProperty(property: JetProperty) {
					visitExplicitDeclaration(property)
					property.acceptChildren(this)
				}
			
				override fun visitPropertyAccessor(accessor: JetPropertyAccessor) {
					visitExplicitDeclaration(accessor)
				}
			
				override fun visitSecondaryConstructor(constructor: JetSecondaryConstructor) {
					visitExplicitDeclaration(constructor)
				}
			
				override fun visitPrimaryConstructor(constructor: JetPrimaryConstructor) {
					visitExplicitDeclaration(constructor)
				}
			
//				Check primary constructor when there are no secondary constructors
				override fun visitClass(jetClass: JetClass) {
					if (equalsJvmSignature(jetClass, element) && (jetClass.getFqName() == element.getDeclaringType().getFqName())) {
						result.add(jetClass)
						return
					}
					
					jetClass.acceptChildren(this)
				}
			
				fun visitExplicitDeclaration(declaration: JetDeclaration) {
					if (equalsJvmSignature(declaration, element) && equalsDeclaringTypes(declaration, element)) {
						result.add(declaration)
					}
				}
			}
		else -> null
	}
}

fun IType.getFqName(): FqName {
	return FqName(this.getFullyQualifiedName('.'))
}

fun equalsJvmSignature(jetElement: JetElement, javaMember: IMember): Boolean {
	val jetSignatures = jetElement.getUserData(LightClassBuilderFactory.JVM_SIGNATURE)
	if (jetSignatures == null) return false
	
	val memberSignature = when (javaMember) {
		is IField -> javaMember.getTypeSignature().replace("\\.".toRegex(), "/") // Hack
		is IMethod -> javaMember.getSignature()
		else -> null
	}
	
	return jetSignatures.any { 
		if (it.first == memberSignature) {
			return@any when {
				javaMember is IMethod && javaMember.isConstructor() -> 
					jetElement is JetClass || jetElement is JetConstructor<*>
				else -> it.second == javaMember.getElementName()
			}
		}
		
		false
	}
}

fun equalsDeclaringTypes(jetElement: JetElement, javaMember: IMember): Boolean  {
	val parent = PsiTreeUtil.getParentOfType(jetElement, javaClass<JetClassOrObject>(), javaClass<JetFile>())
	
	val jetFqName = when (parent) {
		is JetClassOrObject -> parent.getFqName()
		is JetFile -> PackageClassUtils.getPackageClassFqName(parent.getPackageFqName())
		else -> null
	}
	
	return jetFqName == javaMember.getDeclaringType().getFqName()
}

open class JetAllVisitor() : JetVisitorVoid() {
	override fun visitElement(element: PsiElement) {
		element.acceptChildren(this)
	}
}