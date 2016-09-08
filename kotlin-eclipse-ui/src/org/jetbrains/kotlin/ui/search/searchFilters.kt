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
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant.SearchElement
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant.SearchElement.JavaSearchElement
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant.SearchElement.KotlinSearchElement

interface SearchFilter {
    fun isApplicable(jetElement: KtElement): Boolean
}

interface SearchFilterAfterResolve {
    fun isApplicable(sourceElement: KtElement, originElement: KtElement): Boolean
    
    fun isApplicable(sourceElement: IJavaElement, originElement: IJavaElement): Boolean
    
    fun isApplicable(sourceElements: List<SourceElement>, originElement: SearchElement): Boolean {
        val (javaElements, kotlinElements) = getJavaAndKotlinElements(sourceElements)
        return when (originElement) {
            is JavaSearchElement -> javaElements.any { isApplicable(it, originElement.javaElement) }
            is KotlinSearchElement -> kotlinElements.any { isApplicable(it, originElement.kotlinElement) }
        }
    }
}

fun getBeforeResolveFilters(querySpecification: QuerySpecification): List<SearchFilter> {
    val filters = arrayListOf<SearchFilter>()
    if (querySpecification.getLimitTo() == IJavaSearchConstants.REFERENCES) {
        filters.add(NonImportFilter())
        filters.add(ReferenceFilter())
    }
    
    return filters
}

fun getAfterResolveFilters(): List<SearchFilterAfterResolve> = listOf(ResolvedReferenceFilter())

class ReferenceFilter : SearchFilter {
    override fun isApplicable(jetElement: KtElement): Boolean = jetElement is KtReferenceExpression
}

class NonImportFilter : SearchFilter {
    override fun isApplicable(jetElement: KtElement): Boolean {
        return jetElement !is KtSimpleNameExpression || !jetElement.isImportDirectiveExpression()
    }
}

class ResolvedReferenceFilter : SearchFilterAfterResolve {
    override fun isApplicable(sourceElement: KtElement, originElement: KtElement): Boolean {
        return sourceElement == originElement
    }
    
    override fun isApplicable(sourceElement: IJavaElement, originElement: IJavaElement): Boolean {
        return  referenceFilter(sourceElement, originElement)
    }
    
    private fun referenceFilter(potentialElement: IJavaElement, originElement: IJavaElement): Boolean {
        return when {
            originElement.isConstructorCall() && potentialElement.isConstructorCall() -> {
                (originElement as IMethod).getDeclaringType() == (potentialElement as IMethod).getDeclaringType()
            }
            
            originElement.isConstructorCall() -> {
                (originElement as IMethod).getDeclaringType() == potentialElement
            }
            
            potentialElement.isConstructorCall() -> {
                originElement == (potentialElement as IMethod).getDeclaringType()
            }
            
            else -> potentialElement == originElement
        }
    }
    
    private fun IJavaElement.isConstructorCall() = this is IMethod && this.isConstructor()
}