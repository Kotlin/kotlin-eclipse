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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.utils.addToStdlib.constant
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtSuperExpression

public val FILE_PROJECT: Key<IJavaProject> = Key.create("FILE_PROJECT")

public fun List<KotlinReference>.resolveToSourceElements(): List<SourceElement> {
    if (isEmpty()) return emptyList()
    
    val jetFile = first().expression.getContainingKtFile()
    val javaProject = JavaCore.create(KotlinPsiManager.getEclipseFile(jetFile)?.getProject()) 
                      ?: jetFile.getUserData(FILE_PROJECT)
    if (javaProject == null) return emptyList()

    return resolveToSourceElements(
                KotlinAnalyzer.analyzeFile(jetFile).analysisResult.bindingContext,
                javaProject)
}

public fun List<KotlinReference>.resolveToSourceElements(context: BindingContext, javaProject: IJavaProject): List<SourceElement> {
    return flatMap { it.getTargetDescriptors(context) }
            .flatMap { EclipseDescriptorUtils.descriptorToDeclarations(it, javaProject.project) }
}

public fun getReferenceExpression(element: PsiElement): KtReferenceExpression? {
	return PsiTreeUtil.getNonStrictParentOfType(element, KtReferenceExpression::class.java)
}

public fun KtElement.resolveToSourceDeclaration(): List<SourceElement> {
    val jetElement = this
    return when (jetElement) {
        is KtDeclaration -> {
            listOf(KotlinSourceElement(jetElement))
        }
        
        else -> {
            // Try search declaration by reference
            val referenceExpression = getReferenceExpression(jetElement)
            if (referenceExpression == null) return emptyList()
            
            val reference = createReferences(referenceExpression)
            reference.resolveToSourceElements()
        } 
    }
}

public enum class ReferenceAccess(val isRead: Boolean, val isWrite: Boolean) {
    READ(true, false), WRITE(false, true), READ_WRITE(true, true)
}

public fun KtExpression.readWriteAccess(): ReferenceAccess {
    var expression = getQualifiedExpressionForSelectorOrThis()
    loop@ while (true) {
        val parent = expression.parent
        when (parent) {
            is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLabeledExpression -> expression = parent as KtExpression
            else -> break@loop
        }
    }

    val assignment = expression.getAssignmentByLHS()
    if (assignment != null) {
        when (assignment.operationToken) {
            KtTokens.EQ -> return ReferenceAccess.WRITE
            else ->  return ReferenceAccess.READ_WRITE
        }
    }

    return if ((expression.parent as? KtUnaryExpression)?.operationToken in constant { setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) })
        ReferenceAccess.READ_WRITE
    else
        ReferenceAccess.READ
}

// TODO: obtain this function from referenceUtil.kt (org.jetbrains.kotlin.idea.references)
fun KotlinReference.canBeResolvedViaImport(target: DeclarationDescriptor): Boolean {
    if (!target.canBeReferencedViaImport()) return false
    if (target.isExtension) return true // assume that any type of reference can use imports when resolved to extension
    val referenceExpression = this.expression as? KtNameReferenceExpression ?: return false
    if (CallTypeAndReceiver.detect(referenceExpression).receiver != null) return false
    if (expression.parent is KtThisExpression || expression.parent is KtSuperExpression) return false // TODO: it's a bad design of PSI tree, we should change it
    return true
}