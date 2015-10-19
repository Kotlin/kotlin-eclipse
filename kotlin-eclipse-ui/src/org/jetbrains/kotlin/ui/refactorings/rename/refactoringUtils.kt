package org.jetbrains.kotlin.ui.refactorings.rename

import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetReferenceExpression

fun getLengthOfIdentifier(jetElement: JetElement): Int? {
    return when (jetElement) {
        is JetNamedDeclaration -> jetElement.getNameIdentifier()!!.getTextLength()
        is JetReferenceExpression -> jetElement.getTextLength()
        else -> null
    }
}