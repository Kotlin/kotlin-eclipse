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
package org.jetbrains.kotlin.ui.search

import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.ui.search.QuerySpecification
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinFindImplementationsInProjectAction
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant.SearchElement
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant.SearchElement.JavaSearchElement
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant.SearchElement.KotlinSearchElement
import org.jetbrains.kotlin.utils.findCurrentDescriptor

interface SearchFilter {
    fun isApplicable(jetElement: KtElement): Boolean
}

interface SearchFilterAfterResolve {
    fun isApplicable(sourceElement: KtElement, originElement: KtElement): Boolean

    fun isApplicable(sourceElement: IJavaElement, originElement: IJavaElement): Boolean

    fun isApplicable(
        sourceElements: List<SourceElement>,
        originElement: SearchElement
    ): Boolean {
        val (javaElements, kotlinElements) = getJavaAndKotlinElements(sourceElements)
        return when (originElement) {
            is JavaSearchElement -> javaElements.any { isApplicable(it, originElement.javaElement) }
            is KotlinSearchElement -> kotlinElements.any { isApplicable(it, originElement.kotlinElement) }
        }
    }
}

fun getBeforeResolveFilters(querySpecification: QuerySpecification): List<SearchFilter> =
    when (querySpecification.limitTo) {
        IJavaSearchConstants.REFERENCES -> listOf(NonImportFilter, ElementWithPossibleReferencesFilter)
        KotlinFindImplementationsInProjectAction.IMPLEMENTORS_LIMIT_TO -> listOf(
            NonImportFilter,
            PossibleOverridingMemberFilter
        )
        else -> emptyList()
    }

fun getAfterResolveFilters(querySpecification: QuerySpecification): List<SearchFilterAfterResolve> =
    when (querySpecification.limitTo) {
        KotlinFindImplementationsInProjectAction.IMPLEMENTORS_LIMIT_TO -> listOf(InheritorsFilter)
        else -> listOf(ResolvedReferenceFilter)
    }

object ElementWithPossibleReferencesFilter : SearchFilter {
    override fun isApplicable(jetElement: KtElement): Boolean =
        jetElement is KtReferenceExpression || (jetElement is KtPropertyDelegate)
}

object PossibleOverridingMemberFilter : SearchFilter {

    override fun isApplicable(jetElement: KtElement): Boolean {
        return jetElement is KtClass || jetElement is KtNamedFunction || jetElement is KtProperty || jetElement is KtObjectDeclaration
    }
}

object NonImportFilter : SearchFilter {
    override fun isApplicable(jetElement: KtElement): Boolean {
        return jetElement !is KtSimpleNameExpression || !jetElement.isImportDirectiveExpression()
    }
}

object InheritorsFilter : SearchFilterAfterResolve {
    override fun isApplicable(sourceElement: KtElement, originElement: KtElement): Boolean {
        if (originElement is KtClass && (sourceElement !is KtClass && sourceElement !is KtObjectDeclaration)) return false
        if (originElement is KtProperty && sourceElement !is KtProperty) return false
        if (originElement is KtNamedFunction && sourceElement !is KtNamedFunction) return false

        val (tempSourceModuleDescriptor, tempSourceDescriptor) = sourceElement.tryGetDescriptor()
        val (_, tempOriginDescriptor) = originElement.tryGetDescriptor()

        if (tempSourceDescriptor == null || tempOriginDescriptor == null) return false

        val tempCurrentOriginDescriptor =
            tempSourceModuleDescriptor.findCurrentDescriptor(tempOriginDescriptor) ?: return false
        val tempCurrentSourceDescriptor =
            tempSourceModuleDescriptor.findCurrentDescriptor(tempSourceDescriptor) ?: return false

        if (tempCurrentOriginDescriptor == tempCurrentSourceDescriptor) return false

        return if (tempCurrentSourceDescriptor is ClassDescriptor && tempCurrentOriginDescriptor is ClassDescriptor) {
            return tempCurrentSourceDescriptor.isSubclassOf(tempCurrentOriginDescriptor)
        } else {
            val tempOverriddenDescriptors = when (tempSourceDescriptor) {
                is FunctionDescriptor -> tempSourceDescriptor.overriddenTreeUniqueAsSequence(false).toList()
                is PropertyDescriptor -> tempSourceDescriptor.overriddenTreeUniqueAsSequence(false).toList()
                else -> return false
            }.mapNotNull {
                tempSourceModuleDescriptor.findCurrentDescriptor(it)
            }

            tempSourceModuleDescriptor.findCurrentDescriptor(tempOriginDescriptor) in tempOverriddenDescriptors
        }
    }

    private fun KtElement.tryGetDescriptor(): Pair<ModuleDescriptor, DeclarationDescriptor?> {
        val (bindingContext, moduleDescriptor) = KotlinAnalyzer.analyzeFile(containingKtFile).analysisResult
        return Pair(moduleDescriptor, bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this])
    }

    override fun isApplicable(sourceElement: IJavaElement, originElement: IJavaElement): Boolean = false
}

object ResolvedReferenceFilter : SearchFilterAfterResolve {
    override fun isApplicable(sourceElement: KtElement, originElement: KtElement): Boolean {
        return sourceElement == originElement || InheritorsFilter.isApplicable(sourceElement, originElement)
    }

    override fun isApplicable(sourceElement: IJavaElement, originElement: IJavaElement): Boolean {
        return referenceFilter(sourceElement, originElement)
    }

    private fun referenceFilter(potentialElement: IJavaElement, originElement: IJavaElement): Boolean {
        return when {
            originElement.isConstructorCall() && potentialElement.isConstructorCall() -> {
                (originElement as IMethod).declaringType == (potentialElement as IMethod).declaringType
            }

            originElement.isConstructorCall() -> {
                (originElement as IMethod).declaringType == potentialElement
            }

            potentialElement.isConstructorCall() -> {
                originElement == (potentialElement as IMethod).declaringType
            }

            else -> potentialElement == originElement
        }
    }

    private fun IJavaElement.isConstructorCall() = this is IMethod && isConstructor
}
