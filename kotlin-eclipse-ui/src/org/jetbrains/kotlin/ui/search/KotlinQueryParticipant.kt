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

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.eclipse.jdt.ui.search.IMatchPresentation
import org.eclipse.jdt.ui.search.IQueryParticipant
import org.eclipse.jdt.ui.search.ISearchRequestor
import org.eclipse.jdt.ui.search.QuerySpecification
import org.eclipse.search.internal.ui.text.FileSearchQuery
import org.eclipse.search.ui.ISearchResult
import org.eclipse.search.ui.text.FileTextSearchScope
import org.eclipse.core.resources.ResourcesPlugin
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import com.intellij.psi.PsiElement
import org.eclipse.search.internal.ui.text.FileSearchResult
import org.jetbrains.kotlin.eclipse.ui.utils.findElementByDocumentOffset
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import java.util.ArrayList
import org.jetbrains.kotlin.core.references.KotlinReference
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache
import org.eclipse.search.ui.text.Match
import org.eclipse.jface.viewers.ILabelProvider
import org.jetbrains.kotlin.ui.editors.outline.PsiLabelProvider
import org.eclipse.jface.viewers.LabelProvider
import org.jetbrains.kotlin.psi.KtElement
import org.eclipse.jdt.internal.core.JavaModel
import org.eclipse.core.resources.IProject
import org.jetbrains.kotlin.core.references.createReferences
import org.eclipse.core.runtime.IAdaptable
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.core.asJava.getDeclaringTypeFqName
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IMethod
import org.eclipse.jdt.core.IMember
import org.eclipse.jdt.core.search.SearchPattern
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.IField
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.eclipse.jface.util.SafeRunnable
import org.eclipse.core.runtime.ISafeRunnable
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery
import org.eclipse.jdt.internal.ui.search.AbstractJavaSearchResult
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinScopedQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinJavaQuerySpecification
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinOnlyQuerySpecification
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinAndJavaSearchable
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinScoped
import org.jetbrains.kotlin.psi.KtConstructor
import org.eclipse.search2.internal.ui.text2.DefaultTextSearchQueryProvider
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput

public class KotlinQueryParticipant : IQueryParticipant {
    override public fun search(requestor: ISearchRequestor, querySpecification: QuerySpecification, monitor: IProgressMonitor?) {
        SafeRunnable.run(object : ISafeRunnable {
            override fun run() {
                val searchElements = getSearchElements(querySpecification)
                if (searchElements.isEmpty()) return
                
                if (querySpecification is KotlinAndJavaSearchable) {
                    runCompositeSearch(searchElements, requestor, querySpecification, monitor)
                    return
                }
                
                val kotlinFiles = getKotlinFilesByScope(querySpecification)
                if (kotlinFiles.isEmpty()) return
                
                if (searchElements.size > 1) {
                    KotlinLogger.logWarning("There are more than one elements to search: $searchElements")
                }
                
                // We assume that there is only one search element, it could be IJavaElement or KtElement
                val searchElement = searchElements.first()
                val searchResult = searchTextOccurrences(searchElement, kotlinFiles)
                if (searchResult == null) return
                
                val elements = obtainElements(searchResult as FileSearchResult, kotlinFiles)
                val matchedReferences = resolveElementsAndMatch(elements, searchElement, querySpecification)
                
                matchedReferences.forEach { requestor.reportMatch(KotlinElementMatch(it)) }
            }
            
            override fun handleException(exception: Throwable) {
                KotlinLogger.logError(exception)
            }
        })
    }
    
    override public fun estimateTicks(specification: QuerySpecification): Int = 500
    
    override public fun getUIParticipant() = KotlinReferenceMatchPresentation()
    
    private fun runCompositeSearch(elements: List<SearchElement>, requestor: ISearchRequestor, originSpecification: QuerySpecification, 
            monitor: IProgressMonitor?) {
        
        fun reportSearchResults(result: AbstractJavaSearchResult) {
            for (searchElement in result.getElements()) {
                result.getMatches(searchElement).forEach { requestor.reportMatch(it) }
            }
        }
        
        val specifications = elements.map { searchElement -> 
            when (searchElement) {
                is SearchElement.JavaSearchElement -> 
                    ElementQuerySpecification(
                        searchElement.javaElement, 
                        originSpecification.getLimitTo(), 
                        originSpecification.getScope(), 
                        originSpecification.getScopeDescription())
                
                is SearchElement.KotlinSearchElement -> 
                    KotlinOnlyQuerySpecification(
                        searchElement.kotlinElement,
                        originSpecification.getFilesInScope(), 
                        originSpecification.getLimitTo(), 
                        originSpecification.getScopeDescription())
            }
        }
        
        if (originSpecification is KotlinScoped) {
            for (specification in specifications) {
                KotlinQueryParticipant().search({ requestor.reportMatch(it) }, specification, monitor)
            }
        } else {
            for (specification in specifications) {
                val searchQuery = JavaSearchQuery(specification)
                searchQuery.run(monitor)
                reportSearchResults(searchQuery.getSearchResult() as AbstractJavaSearchResult)
            }
        }
    }
    
    sealed class SearchElement private constructor() {
        abstract fun getSearchText(): String?
        
        class JavaSearchElement(val javaElement: IJavaElement) : SearchElement() {
            override fun getSearchText(): String = javaElement.getElementName()
        }
        
        class KotlinSearchElement(val kotlinElement: KtElement) : SearchElement() {
            override fun getSearchText(): String? = kotlinElement.getName()
        }
    }
    
    
    private fun getSearchElements(querySpecification: QuerySpecification): List<SearchElement> {
        fun obtainSearchElements(sourceElements: List<SourceElement>): List<SearchElement> {
            val (javaElements, kotlinElements) = getJavaAndKotlinElements(sourceElements)
            return javaElements.map { SearchElement.JavaSearchElement(it) } + 
                   kotlinElements.map { SearchElement.KotlinSearchElement(it) }
                   
        }
        
        return when (querySpecification) {
            is ElementQuerySpecification -> listOf(SearchElement.JavaSearchElement(querySpecification.getElement()))
            is KotlinOnlyQuerySpecification -> listOf(SearchElement.KotlinSearchElement(querySpecification.kotlinElement))
            is KotlinAndJavaSearchable -> obtainSearchElements(querySpecification.sourceElements)
            else -> emptyList()
        }
    }
    
    private fun searchTextOccurrences(searchElement: SearchElement, filesScope: List<IFile>): ISearchResult? {
        val searchText = searchElement.getSearchText()
        if (searchText == null) return null
        
        val scope = FileTextSearchScope.newSearchScope(filesScope.toTypedArray(), null as Array<String?>?, false)
        
        val query = DefaultTextSearchQueryProvider().createQuery(object : TextSearchInput() {
            override fun isWholeWordSearch(): Boolean = true
        
            override fun getSearchText(): String = searchText
            
            override fun isCaseSensitiveSearch(): Boolean = true
            
            override fun isRegExSearch(): Boolean = false
            
            override fun getScope(): FileTextSearchScope = scope
        })
        
        query.run(null)
        
        return query.getSearchResult()
    }
    
    private fun resolveElementsAndMatch(elements: List<KtElement>, searchElement: SearchElement, 
            querySpecification: QuerySpecification): List<KtElement> {
        val beforeResolveFilters = getBeforeResolveFilters(querySpecification)
        val afterResolveFilters = getAfterResolveFilters()
        
        // This is important for optimization: 
        // we will consequentially cache files one by one which are containing these references
        val sortedByFileNameElements = elements.sortedBy { it.getContainingKtFile().getName() }
        
        return sortedByFileNameElements.filter { element ->
            val beforeResolveCheck = beforeResolveFilters.all { it.isApplicable(element) }
            if (!beforeResolveCheck) return@filter false
            
            val sourceElements = element.resolveToSourceDeclaration()
            if (sourceElements.isEmpty()) return@filter false
            
            val additionalElements = getContainingClassOrObjectForConstructor(sourceElements)
            
            return@filter afterResolveFilters.all { it.isApplicable(sourceElements, searchElement) } ||
                    afterResolveFilters.all { it.isApplicable(additionalElements, searchElement) }
        }
    }
    
    private fun obtainElements(searchResult: FileSearchResult, files: List<IFile>): List<KtElement> {
        val elements = ArrayList<KtElement>()
        for (file in files) {
            val matches = searchResult.getMatches(file)
            val jetFile = KotlinPsiManager.getParsedFile(file)
            val document = EditorUtil.getDocument(file)
            
            matches
                .map { 
                    val element = jetFile.findElementByDocumentOffset(it.getOffset(), document) 
                    element?.let { PsiTreeUtil.getNonStrictParentOfType(it, KtElement::class.java) }
                }
                .filterNotNullTo(elements)
        }
        
        return elements
    }
    
    private fun getKotlinFilesByScope(querySpecification: QuerySpecification): List<IFile> {
        return when (querySpecification) {
            is ElementQuerySpecification,
            is KotlinJavaQuerySpecification -> querySpecification.getScope().getKotlinFiles()
            is KotlinScoped -> querySpecification.searchScope
            else -> emptyList()
        }
    }
}

fun getContainingClassOrObjectForConstructor(sourceElements: List<SourceElement>): List<SourceElement> {
    return sourceElements.mapNotNull {
        if (it is KotlinSourceElement) {
            val psi = it.psi
            if (psi is KtConstructor<*>) {
                return@mapNotNull KotlinSourceElement(psi.getContainingClassOrObject())
            }
        }
        
        null
    }
}

fun getJavaAndKotlinElements(sourceElements: List<SourceElement>): Pair<List<IJavaElement>, List<KtElement>> {
    val javaElements = sourceElementsToLightElements(sourceElements)
    
    // Filter out Kotlin elements which have light elements because Javas search will call KotlinQueryParticipant
    // to look up for these elements
    val kotlinElements = sourceElementsToKotlinElements(sourceElements).filterNot { kotlinElement -> 
        javaElements.any { it.getElementName() == kotlinElement.getName() }
    }
    
    return Pair(javaElements, kotlinElements)
}

private fun sourceElementsToKotlinElements(sourceElements: List<SourceElement>): List<KtElement> {
    return sourceElements
            .filterIsInstance(KotlinSourceElement::class.java)
            .map { it.psi }
}

fun IJavaSearchScope.getKotlinFiles(): List<IFile> {
    return enclosingProjectsAndJars()
            .map { JavaModel.getTarget(it, true) }
            .filterIsInstance(IProject::class.java)
            .flatMap { KotlinPsiManager.getFilesByProject(it) }
}

fun QuerySpecification.getFilesInScope(): List<IFile> {
    return when (this) {
        is KotlinScoped -> this.searchScope
        else -> this.scope.getKotlinFiles()
    }
}

public class KotlinElementMatch(val jetElement: KtElement) : Match(KotlinAdaptableElement(jetElement), jetElement.getTextOffset(), 
        jetElement.getTextOffset())

class KotlinAdaptableElement(val jetElement: KtElement): IAdaptable {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getAdapter(adapter: Class<T>?): T? {
        return when {
            IResource::class.java == adapter ->
                KotlinPsiManager.getEclipseFile(jetElement.getContainingKtFile()) as T
            else -> null
        }
    }
}