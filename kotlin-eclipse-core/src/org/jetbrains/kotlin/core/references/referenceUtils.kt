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
import org.jetbrains.kotlin.psi.JetReferenceExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.jdt.core.IJavaElement
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.psi.JetObjectDeclarationName
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.core.model.toLightElements
import org.jetbrains.kotlin.core.log.KotlinLogger

public val FILE_PROJECT: Key<IJavaProject> = Key.create("FILE_PROJECT")

public fun KotlinReference.resolveToSourceElements(): List<SourceElement> {
    val jetFile = expression.getContainingJetFile()
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

public fun getReferenceExpression(element: PsiElement): JetReferenceExpression? {
	return PsiTreeUtil.getNonStrictParentOfType(element, JetReferenceExpression::class.java)
}

sealed class VisibilityScopeDeclaration private constructor() {
    // Represents Java elements and Kotlin light elements 
    class JavaAndKotlinScopeDeclaration(
            val javaElements: List<IJavaElement>, 
            val kotlinElements: List<JetDeclaration> = emptyList()) : VisibilityScopeDeclaration() {
    }
    
    class KotlinOnlyScopeDeclaration(val jetDeclaration: JetDeclaration) : VisibilityScopeDeclaration() {
        override fun hashCode(): Int = jetDeclaration.hashCode()
    
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is KotlinOnlyScopeDeclaration) return false
            return jetDeclaration == other.jetDeclaration
        }
    }
    
    object NoDeclaration : VisibilityScopeDeclaration()
}

public fun JetElement.resolveToSourceDeclaration(javaProject: IJavaProject): VisibilityScopeDeclaration {
    val jetElement = this
    return when (jetElement) {
        is JetObjectDeclarationName -> {
            val objectDeclaration = PsiTreeUtil.getParentOfType(jetElement, JetObjectDeclaration::class.java)
            objectDeclaration?.let { it.resolveToSourceDeclaration(javaProject) } ?: VisibilityScopeDeclaration.NoDeclaration
        }
        
        is JetDeclaration -> {
            val lightElements = jetElement.toLightElements(javaProject)
            if (lightElements.isNotEmpty()) {
                val lightElementNames = lightElements.map { it.getElementName() }
                val kotlinElement = if (jetElement.getName() !in lightElementNames) {
                    listOf(jetElement)
                } else {
                    emptyList()
                }
                VisibilityScopeDeclaration.JavaAndKotlinScopeDeclaration(lightElements, kotlinElement)
            } else {
                // Element should present only in Kotlin as there is no corresponding light element
                VisibilityScopeDeclaration.KotlinOnlyScopeDeclaration(jetElement)
            }
        }
        
        else -> {
            // Try search usages by reference
            val referenceExpression = getReferenceExpression(jetElement)
            if (referenceExpression == null) return VisibilityScopeDeclaration.NoDeclaration
            
            val reference = createReference(referenceExpression)
            val sourceElements = reference.resolveToSourceElements()
            if (sourceElements.isEmpty()) return VisibilityScopeDeclaration.NoDeclaration
            
            val lightElements = sourceElementsToLightElements(sourceElements, javaProject)
            if (lightElements.isNotEmpty()) {
                val lightElementNames = lightElements.map { it.getElementName() }
                val kotlinElements = sourceElements
                        .filterIsInstance(KotlinSourceElement::class.java)
                        .filter { it.psi.getName() !in lightElementNames }
                        .map { it.psi as JetDeclaration }
                VisibilityScopeDeclaration.JavaAndKotlinScopeDeclaration(lightElements, kotlinElements)
            } else {
                if (sourceElements.size() > 1) {
                    KotlinLogger.logWarning("There are more than one elements for ${referenceExpression.getText()}")
                }
                
                sourceElements.firstOrNull { it is KotlinSourceElement }?.let { 
                    VisibilityScopeDeclaration.KotlinOnlyScopeDeclaration((it as KotlinSourceElement).psi as JetDeclaration)
                } ?: VisibilityScopeDeclaration.NoDeclaration
            }
        } 
    }
}