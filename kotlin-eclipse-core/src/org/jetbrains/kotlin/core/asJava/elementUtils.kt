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
package org.jetbrains.kotlin.core.asJava

import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.jdt.core.IMember
import org.eclipse.jdt.core.IField
import org.eclipse.jdt.core.IMethod
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetConstructor
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetClassOrObject
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.name.FqName
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider

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

fun getDeclaringTypeFqName(jetElement: JetElement): KotlinClassNameInfo {
    val parent = PsiTreeUtil.getParentOfType(jetElement, JetClassOrObject::class.java, JetFile::class.java)
    return if (parent != null) getTypeFqName(parent) else KotlinClassNameInfo.EMPTY
}

fun getTypeFqName(element: PsiElement): KotlinClassNameInfo {
    return when (element) {
        is JetClassOrObject -> KotlinClassNameInfo(element.getFqName())
        is JetFile -> 
            KotlinClassNameInfo(PackageClassUtils.getPackageClassFqName(element.getPackageFqName()), 
                NoResolveFileClassesProvider.getFileClassInfo(element).fileClassFqName)
        else -> KotlinClassNameInfo.EMPTY
    }
}

data class KotlinClassNameInfo(val className: FqName? = null, val filePartName: FqName? = null) {
    companion object {
        val EMPTY = KotlinClassNameInfo()
    }
}