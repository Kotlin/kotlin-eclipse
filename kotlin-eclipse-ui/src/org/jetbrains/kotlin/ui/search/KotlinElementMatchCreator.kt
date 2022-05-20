package org.jetbrains.kotlin.ui.search

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jdt.ui.search.QuerySpecification
import org.eclipse.search.ui.text.Match
import org.jetbrains.kotlin.core.utils.getBindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinFindImplementationsInProjectAction

interface KotlinElementMatchCreator {

    fun createMatch(element: KtElement): Match?

    companion object {
        fun getMatchCreator(querySpecification: QuerySpecification) = when (querySpecification.limitTo) {
            KotlinFindImplementationsInProjectAction.IMPLEMENTORS_LIMIT_TO -> InheritorsSearchMatchCreator
            else -> BasicSearchMatchCreator
        }
    }
}

object InheritorsSearchMatchCreator : KotlinElementMatchCreator {
    override fun createMatch(element: KtElement): Match? {
        val (descriptor: DeclarationDescriptor?, nameIdentifier) = when (element) {
            is KtNamedFunction -> element.resolveToDescriptorIfAny() to element.nameIdentifier
            is KtClass -> element.resolveToDescriptorIfAny() to element.nameIdentifier
            is KtProperty -> element.resolveToDescriptorIfAny() to element.nameIdentifier
            is KtObjectDeclaration -> element.resolveToDescriptorIfAny() to element.nameIdentifier
            else -> return null
        }
        val tempLabel =
            descriptor?.let { DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(it) } ?: BasicSearchMatchCreator.render(
                element
            )

        return KotlinElementMatch(element, tempLabel, nameIdentifier ?: element)
    }

}

object BasicSearchMatchCreator : KotlinElementMatchCreator {

    override fun createMatch(element: KtElement): Match {
        return KotlinElementMatch(element, render(element), element)
    }

    fun render(element: KtElement): String {
        val tempElement = element.getCall(element.getBindingContext())?.toString() ?: element.text

        val tempParentDescriptor =
            PsiTreeUtil.getParentOfType(element, KtDeclaration::class.java)?.resolveToDescriptorIfAny()

        return buildString {
            append(tempElement.lines().first())
            if (tempParentDescriptor != null) {
                append(" in ")
                append(DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions {
                    modifiers = emptySet()
                    includeAdditionalModifiers = false
                }.render(tempParentDescriptor))
            }
        }
    }

}
