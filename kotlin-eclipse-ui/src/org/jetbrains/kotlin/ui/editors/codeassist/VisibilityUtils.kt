package org.jetbrains.kotlin.ui.editors.codeassist

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

// from idea/idea-core/src/org/jetbrains/kotlin/idea/core/Utils.kt but without the second parameter
public fun PsiElement.getResolutionScope(bindingContext: BindingContext): LexicalScope {
    for (parent in parentsWithSelf) {
        if (parent is KtElement) {
            val scope = bindingContext[BindingContext.LEXICAL_SCOPE, parent]
            if (scope != null) return scope
        }

        if (parent is KtClassBody) {
            val classDescriptor = bindingContext[BindingContext.CLASS, parent.getParent()] as? ClassDescriptorWithResolutionScopes
            if (classDescriptor != null) {
                return classDescriptor.getScopeForMemberDeclarationResolution()
            }
        }
    }
    error("Not in JetFile")
}

//from idea/idea-core/src/org/jetbrains/kotlin/idea/core/descriptorUtils.kt
public fun DeclarationDescriptorWithVisibility.isVisible(
    from: DeclarationDescriptor,
    bindingContext: BindingContext? = null,
    element: KtSimpleNameExpression? = null
): Boolean {
    if (Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, this, from)) return true

    if (bindingContext == null || element == null) return false

    val receiverExpression = element.getReceiverExpression()
    if (receiverExpression != null) {
        val receiverType = bindingContext.getType(receiverExpression) ?: return false
        val explicitReceiver = ExpressionReceiver.create(receiverExpression, receiverType, bindingContext)
        return Visibilities.isVisible(explicitReceiver, this, from)
    }
    else {
        val resolutionScope = element.getResolutionScope(bindingContext)
        return resolutionScope.getImplicitReceiversHierarchy().any {
            Visibilities.isVisible(it.value, this, from)
        }
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