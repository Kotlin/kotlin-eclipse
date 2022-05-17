package org.jetbrains.kotlin.ui.search

import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.ui.search.QuerySpecification
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinFindImplementationsInProjectAction

fun interface SearchParentObjectMapper {

    fun map(element: KtElement): KtElement?

    companion object {
        fun getMapper(querySpecification: QuerySpecification): SearchParentObjectMapper =
            when (querySpecification.limitTo) {
                IJavaSearchConstants.REFERENCES -> ReferencesParentObjectMapper
                KotlinFindImplementationsInProjectAction.IMPLEMENTORS_LIMIT_TO -> ImplementationsParentObjectMapper
                else -> NO_MAPPING
            }

        private val NO_MAPPING = SearchParentObjectMapper { null }
    }
}

object ReferencesParentObjectMapper : SearchParentObjectMapper {
    override fun map(element: KtElement): KtElement? = element.getParentOfType<KtReferenceExpression>(false)
}

object ImplementationsParentObjectMapper : SearchParentObjectMapper {
    override fun map(element: KtElement): KtElement? =
        element.getParentOfTypes(
            false,
            KtClass::class.java,
            KtObjectDeclaration::class.java,
            KtProperty::class.java,
            KtNamedFunction::class.java
        )
}
