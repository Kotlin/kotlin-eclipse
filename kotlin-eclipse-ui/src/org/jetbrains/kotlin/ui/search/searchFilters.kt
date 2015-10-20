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

import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.eclipse.jdt.ui.search.QuerySpecification
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinLocalQuerySpecification
import org.jetbrains.kotlin.descriptors.SourceElement
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.jetbrains.kotlin.core.model.toLightElements
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.KotlinOnlyScopeDeclaration
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.JavaAndKotlinScopeDeclaration
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinQuerySpecification
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.NoDeclaration

interface SearchFilter {
    fun isApplicable(jetElement: JetElement): Boolean
}

interface SearchFilterAfterResolve {
    fun isApplicable(declaration: KotlinOnlyScopeDeclaration, originalDeclaration: KotlinOnlyScopeDeclaration): Boolean
    
    fun isApplicable(declaration: JavaAndKotlinScopeDeclaration, originalElement: IJavaElement): Boolean
    
    fun isApplicable(declaration: VisibilityScopeDeclaration, originDeclaration: VisibilityScopeDeclaration): Boolean {
        return when (declaration) {
            is KotlinOnlyScopeDeclaration -> {
                (originDeclaration as? KotlinOnlyScopeDeclaration)?.let { isApplicable(declaration, it) } ?: false
            }
            
            is JavaAndKotlinScopeDeclaration -> {
                (originDeclaration as? JavaAndKotlinScopeDeclaration)?.let { origin -> 
                    origin.javaElements.any { isApplicable(declaration, it) }
                } ?: false
            }
            
            is NoDeclaration -> false
        }
    }
    
    fun isApplicable(sourceDeclaration: VisibilityScopeDeclaration, query: QuerySpecification): Boolean {
        return when (query) {
            is ElementQuerySpecification -> {
                if (sourceDeclaration is JavaAndKotlinScopeDeclaration) {
                    isApplicable(sourceDeclaration, query.getElement())
                } else {
                    false
                }
            }
            
            is KotlinLocalQuerySpecification -> {
                if (sourceDeclaration is KotlinOnlyScopeDeclaration) {
                    isApplicable(sourceDeclaration, query.kotlinDeclaration)
                } else {
                    false
                }
            }
            
            is KotlinQuerySpecification -> isApplicable(sourceDeclaration, query.sourceDeclaration)
            
            else -> throw IllegalArgumentException("Cannot apply filter for $query")
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

fun getAfterResolveFilters(): List<SearchFilterAfterResolve> {
    return listOf(ResolvedReferenceFilter())
}

class ReferenceFilter : SearchFilter {
    override fun isApplicable(jetElement: JetElement): Boolean = jetElement is JetReferenceExpression
}

class NonImportFilter : SearchFilter {
    override fun isApplicable(jetElement: JetElement): Boolean {
        return jetElement !is JetSimpleNameExpression || !jetElement.isImportDirectiveExpression()
    }
}

class ResolvedReferenceFilter : SearchFilterAfterResolve {
    override fun isApplicable(declaration: KotlinOnlyScopeDeclaration, originalDeclaration: KotlinOnlyScopeDeclaration): Boolean {
        return declaration == originalDeclaration
    }
    
    override fun isApplicable(declaration: JavaAndKotlinScopeDeclaration, originalElement: IJavaElement): Boolean {
        return declaration.javaElements.any { referenceFilter(it, originalElement) }
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