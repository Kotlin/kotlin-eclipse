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

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.core.utils.getBindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.descriptors.accessors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.utils.addToStdlib.constant

val FILE_PROJECT: Key<IJavaProject> = Key.create("FILE_PROJECT")

fun List<KotlinReference<*>>.resolveToSourceElements(ktFile: KtFile): List<SourceElement> {
    if (isEmpty()) return emptyList()

    val javaProject = JavaCore.create(KotlinPsiManager.getEclipseFile(ktFile)?.project)
        ?: ktFile.getUserData(FILE_PROJECT) ?: return emptyList()

    return resolveToSourceElements(ktFile.getBindingContext(), javaProject)
}

fun List<KotlinReference<*>>.resolveToSourceElements(
    context: BindingContext,
    javaProject: IJavaProject
): List<SourceElement> {
    return flatMap { it.getTargetDescriptors(context) }
        .flatMap { EclipseDescriptorUtils.descriptorToDeclarations(it, javaProject.project) }
}

fun getReferenceExpression(element: PsiElement): KtReferenceExpression? {
    return PsiTreeUtil.getNonStrictParentOfType(element, KtReferenceExpression::class.java)
}

fun KtElement.resolveToSourceDeclaration(): List<SourceElement> {
    return when (val jetElement = this) {
        is KtDeclaration -> listOf(KotlinSourceElement(jetElement))
        else -> {
            // Try search declaration by reference
            val referenceExpression = getReferenceExpression(jetElement) ?: jetElement
            val reference = createReferences(referenceExpression)
            reference.resolveToSourceElements(jetElement.containingKtFile)
        }
    }
}

enum class ReferenceAccess(val isRead: Boolean, val isWrite: Boolean) {
    READ(true, false), WRITE(false, true), READ_WRITE(true, true)
}

fun KtExpression.readWriteAccess(): ReferenceAccess {
    var expression = getQualifiedExpressionForSelectorOrThis()
    loop@ while (true) {
        when (val parent = expression.parent) {
            is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLabeledExpression -> expression =
                parent as KtExpression
            else -> break@loop
        }
    }

    val assignment = expression.getAssignmentByLHS()
    if (assignment != null) {
        return when (assignment.operationToken) {
            KtTokens.EQ -> ReferenceAccess.WRITE
            else -> ReferenceAccess.READ_WRITE
        }
    }

    return if ((expression.parent as? KtUnaryExpression)?.operationToken in constant {
            setOf(
                KtTokens.PLUSPLUS,
                KtTokens.MINUSMINUS
            )
        })
        ReferenceAccess.READ_WRITE
    else
        ReferenceAccess.READ
}

// TODO: obtain this function from referenceUtil.kt (org.jetbrains.kotlin.idea.references)
fun KotlinReference<*>.canBeResolvedViaImport(target: DeclarationDescriptor): Boolean {
    if (!target.canBeReferencedViaImport()) return false
    if (target.isExtension) return true // assume that any type of reference can use imports when resolved to extension
    val referenceExpression = this.expression as? KtNameReferenceExpression ?: return false
    if (CallTypeAndReceiver.detect(referenceExpression).receiver != null) return false
    if (expression.parent is KtThisExpression || expression.parent is KtSuperExpression) return false // TODO: it's a bad design of PSI tree, we should change it
    return true
}
