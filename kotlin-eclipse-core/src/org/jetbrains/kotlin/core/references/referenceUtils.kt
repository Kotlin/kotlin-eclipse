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
package org.jetbrains.kotlin.core.references

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.core.references.KotlinReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtElement
import org.eclipse.jdt.core.IJavaElement
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.core.model.toLightElements
import org.jetbrains.kotlin.core.log.KotlinLogger

public val FILE_PROJECT: Key<IJavaProject> = Key.create("FILE_PROJECT")

public fun KotlinReference.resolveToSourceElements(): List<SourceElement> {
    val jetFile = expression.getContainingKtFile()
    val javaProject = JavaCore.create(KotlinPsiManager.getEclispeFile(jetFile)?.getProject()) 
                      ?: jetFile.getUserData(FILE_PROJECT)
    if (javaProject == null) return emptyList()

    return resolveToSourceElements(
                KotlinAnalyzer.analyzeFile(javaProject, jetFile).analysisResult.bindingContext,
                javaProject)
}

public fun KotlinReference.resolveToSourceElements(context: BindingContext, project: IJavaProject): List<SourceElement> {
    return getTargetDescriptors(context).flatMap { EclipseDescriptorUtils.descriptorToDeclarations(it, project) }
}

public fun getReferenceExpression(element: PsiElement): KtReferenceExpression? {
	return PsiTreeUtil.getNonStrictParentOfType(element, KtReferenceExpression::class.java)
}

public fun KtElement.resolveToSourceDeclaration(javaProject: IJavaProject): List<SourceElement> {
    val jetElement = this
    return when (jetElement) {
        is KtDeclaration -> {
            listOf(KotlinSourceElement(jetElement))
        }
        
        else -> {
            // Try search declaration by reference
            val referenceExpression = getReferenceExpression(jetElement)
            if (referenceExpression == null) return emptyList()
            
            val reference = createReference(referenceExpression)
            reference.resolveToSourceElements()
        } 
    }
}