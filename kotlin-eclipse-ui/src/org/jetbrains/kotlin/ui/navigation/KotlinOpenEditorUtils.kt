/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.core.asJava.equalsJvmSignature
import org.jetbrains.kotlin.core.asJava.getDeclaringTypeFqName

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
				visitExplicitDeclaration(declaration)
				declaration.acceptChildren(this)
			}
			
			override fun visitEnumEntry(enumEntry: JetEnumEntry) {
				visitExplicitDeclaration(enumEntry)
			}
            
            override fun visitProperty(property: JetProperty) {
                visitExplicitDeclaration(property)
            }
			
			fun visitExplicitDeclaration(declaration: JetDeclaration) {
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

fun equalsDeclaringTypes(jetElement: JetElement, javaMember: IMember): Boolean  {
	return getDeclaringTypeFqName(jetElement).any { it == javaMember.getDeclaringType().getFqName() }
}

open class JetAllVisitor() : JetVisitorVoid() {
	override fun visitElement(element: PsiElement) {
		element.acceptChildren(this)
	}
}