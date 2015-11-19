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

import org.jetbrains.kotlin.psi.KtElement
import org.eclipse.jdt.core.IMember
import org.eclipse.jdt.core.IField
import org.eclipse.jdt.core.IMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtClassOrObject
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.name.FqName
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider

fun equalsJvmSignature(KtElement: KtElement, javaMember: IMember): Boolean {
    val jetSignatures = KtElement.getUserData(LightClassBuilderFactory.JVM_SIGNATURE)
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
                KtElement is KtClass || KtElement is KtConstructor<*>
                else -> it.second == javaMember.getElementName()
            }
        }
        
        false
    }
}

fun getDeclaringTypeFqName(KtElement: KtElement): FqName? {
    val parent = PsiTreeUtil.getParentOfType(KtElement, KtClassOrObject::class.java, KtFile::class.java)
    return if (parent != null) getTypeFqName(parent) else null
}

fun getTypeFqName(element: PsiElement): FqName? {
    return when (element) {
        is KtClassOrObject -> element.getFqName()
        is KtFile -> NoResolveFileClassesProvider.getFileClassInfo(element).fileClassFqName
        else -> null
    }
}