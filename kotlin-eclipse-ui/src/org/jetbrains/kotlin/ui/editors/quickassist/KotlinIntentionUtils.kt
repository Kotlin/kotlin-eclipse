package org.jetbrains.kotlin.ui.editors.quickassist

import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility

fun JetNamedDeclaration.canRemoveTypeSpecificationByVisibility(bindingContext: BindingContext): Boolean {
    val isOverride = getModifierList()?.hasModifier(JetTokens.OVERRIDE_KEYWORD) ?: false
    if (isOverride) return true

    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
    return descriptor !is DeclarationDescriptorWithVisibility || !descriptor.getVisibility().isPublicAPI
}
