package org.jetbrains.kotlin.ui.editors.codeassist

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.JetScope
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.psi.JetImportDirective
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.psi.JetUnaryExpression
import org.jetbrains.kotlin.psi.JetUserType
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor

// from compiler/frontend/src/org/jetbrains/kotlin/psi/psiUtil/psiUtils.kt
    public val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = sequence(this) { if (it is PsiFile) null else it.getParent() }

// from idea/idea-core/src/org/jetbrains/kotlin/idea/core/Utils.kt but without the second parameter
public fun PsiElement.getResolutionScope(bindingContext: BindingContext): JetScope {
    for (parent in parentsWithSelf) {
        if (parent is JetExpression) {
            val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, parent]
            if (scope != null) return scope
        }

        if (parent is JetClassBody) {
            val classDescriptor = bindingContext[BindingContext.CLASS, parent.getParent()] as? ClassDescriptorWithResolutionScopes
            if (classDescriptor != null) {
                return classDescriptor.getScopeForMemberDeclarationResolution()
            }
        }

        if (parent is JetFile) {
            return bindingContext[BindingContext.FILE_TO_PACKAGE_FRAGMENT, parent].getMemberScope()
        }
    }
    error("Not in JetFile")
}

//from idea/idea-core/src/org/jetbrains/kotlin/idea/core/descriptorUtils.kt
public fun DeclarationDescriptorWithVisibility.isVisible(
from: DeclarationDescriptor,
bindingContext: BindingContext? = null,
element: JetSimpleNameExpression? = null
): Boolean {
    if (Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, this, from)) return true
    if (bindingContext == null || element == null) return false

    val receiver = element.getReceiverExpression()
    val type = receiver?.let { bindingContext.getType(it) }

    val explicitReceiver = if (receiver != null) {
            type?.let { ExpressionReceiver(receiver, it) }
        } else {
            null
        }

    if (explicitReceiver != null) {
        val normalizeReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(explicitReceiver, bindingContext)
        return Visibilities.isVisible(normalizeReceiver, this, from)
    }

    val jetScope = bindingContext[BindingContext.RESOLUTION_SCOPE, element]
    val implicitReceivers = jetScope?.getImplicitReceiversHierarchy()
    if (implicitReceivers != null) {
        for (implicitReceiver in implicitReceivers) {
            val normalizeReceiver = ExpressionTypingUtils.normalizeReceiverValueForVisibility(implicitReceiver.getValue(), bindingContext)
            if (Visibilities.isVisible(normalizeReceiver, this, from)) return true
        }
    }
    return false
}

//from compiler/frontend/src/org/jetbrains/kotlin/psi/psiUtil/jetPsiUtil.kt
public fun JetSimpleNameExpression.getReceiverExpression(): JetExpression? {
    val parent = getParent()
    when {
        parent is JetQualifiedExpression && !isImportDirectiveExpression() -> {
        val receiverExpression = parent.getReceiverExpression()
        // Name expression can't be receiver for itself
        if (receiverExpression != this) {
            return receiverExpression
        }
    }
        parent is JetCallExpression -> {
        //This is in case `a().b()`
        val callExpression = parent
        val grandParent = callExpression.getParent()
        if (grandParent is JetQualifiedExpression) {
            val parentsReceiver = grandParent.getReceiverExpression()
            if (parentsReceiver != callExpression) {
                return parentsReceiver
            }
        }
    }
        parent is JetBinaryExpression && parent.getOperationReference() == this -> {
        return if (parent.getOperationToken() in OperatorConventions.IN_OPERATIONS) parent.getRight() else parent.getLeft()
    }
        parent is JetUnaryExpression && parent.getOperationReference() == this -> {
        return parent.getBaseExpression()!!
    }
        parent is JetUserType -> {
        val qualifier = parent.getQualifier()
        if (qualifier != null) {
            return qualifier.getReferenceExpression()!!
        }
    }
    }
    return null
}

// from compiler/frontend/src/org/jetbrains/kotlin/psi/psiUtil/jetPsiUtil.kt
public fun JetSimpleNameExpression.isImportDirectiveExpression(): Boolean {
    val parent = getParent()
    if (parent == null) {
        return false
    }
        else {
        return parent is JetImportDirective || parent.getParent() is JetImportDirective
    }
}

//from idea/idea-completion/src/org/jetbrains/kotlin/idea/completion/CompletionSession.kt
public fun TypeParameterDescriptor.isVisible(where: DeclarationDescriptor?): Boolean {
    val owner = getContainingDeclaration()
    var parent = where
    while (parent != null) {
        if (parent == owner) return true
        if (parent is ClassDescriptor && !parent.isInner()) return false
        parent = parent.getContainingDeclaration()
    }
    return true
}