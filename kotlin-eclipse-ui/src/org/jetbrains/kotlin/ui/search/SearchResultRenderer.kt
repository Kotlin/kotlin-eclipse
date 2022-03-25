package org.jetbrains.kotlin.ui.search

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jdt.ui.search.QuerySpecification
import org.jetbrains.kotlin.core.utils.getBindingContext
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall

interface SearchResultRenderer {

    fun render(element: KtElement): String

    companion object {
        fun getResultRenderer(querySpecification: QuerySpecification) = when (querySpecification.limitTo) {
            else -> BasicSearchResultRenderer
        }
    }
}

object BasicSearchResultRenderer : SearchResultRenderer {

    override fun render(element: KtElement): String {
        val tempElement = element.getCall(element.getBindingContext())?.toString() ?: element.text

        val tempParentDescriptor =
            PsiTreeUtil.getParentOfType(element, KtDeclaration::class.java)?.resolveToDescriptorIfAny()

        return buildString {
            append(tempElement)
            if (tempParentDescriptor != null) {
                append(" in ")
                append(DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(tempParentDescriptor))
            }
        }
    }

}
