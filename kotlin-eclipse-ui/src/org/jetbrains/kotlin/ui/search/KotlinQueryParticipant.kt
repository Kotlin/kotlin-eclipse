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

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.ISafeRunnable
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.eclipse.jdt.internal.core.JavaModel
import org.eclipse.jdt.internal.ui.search.AbstractJavaSearchResult
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.eclipse.jdt.ui.search.IQueryParticipant
import org.eclipse.jdt.ui.search.ISearchRequestor
import org.eclipse.jdt.ui.search.QuerySpecification
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.jface.util.SafeRunnable
import org.eclipse.search.internal.ui.text.FileSearchResult
import org.eclipse.search.ui.ISearchResult
import org.eclipse.search.ui.text.FileTextSearchScope
import org.eclipse.search.ui.text.Match
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput
import org.eclipse.search2.internal.ui.text2.DefaultTextSearchQueryProvider
import org.eclipse.ui.model.IWorkbenchAdapter
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.findElementByDocumentOffset
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.ui.commands.findReferences.*

class KotlinQueryParticipant : IQueryParticipant {

    private inner class DelegatedUniqueMatchSearchRequestor(
        private val delegate: ISearchRequestor,
        private val querySpecification: QuerySpecification
    ) : ISearchRequestor {

        private val seenMatches: MutableSet<Any> = mutableSetOf()
        override fun reportMatch(match: Match) {
            val tempElement = match.element
            if ((match is KotlinElementMatch && !seenMatches.add(match.jetElement)) || (tempElement is IJavaElement && !seenMatches.add(tempElement))) return

            delegate.reportMatch(match)

            if (querySpecification.limitTo == KotlinFindImplementationsInProjectAction.IMPLEMENTORS_LIMIT_TO && match is KotlinElementMatch && match.jetElement is KtClass) {
                val tempNewSpec = KotlinOnlyQuerySpecification(
                    match.jetElement,
                    querySpecification.getFilesInScope(),
                    KotlinFindImplementationsInProjectAction.IMPLEMENTORS_LIMIT_TO,
                    querySpecification.scopeDescription
                )
                search(this, tempNewSpec, null)
            }
        }
    }

    override fun search(
        requestor: ISearchRequestor,
        querySpecification: QuerySpecification,
        monitor: IProgressMonitor?
    ) {
        SafeRunnable.run(object : ISafeRunnable {
            override fun run() {
                val tempRequestor =
                    requestor as? DelegatedUniqueMatchSearchRequestor ?: DelegatedUniqueMatchSearchRequestor(
                        requestor,
                        querySpecification
                    )
                val searchElements = getSearchElements(querySpecification)
                if (searchElements.isEmpty()) return

                if (querySpecification is KotlinAndJavaSearchable) {
                    runCompositeSearch(searchElements, tempRequestor, querySpecification, monitor)
                    return
                }

                val kotlinFiles = getKotlinFilesByScope(querySpecification)
                if (kotlinFiles.isEmpty()) return
                if (monitor?.isCanceled == true) return

                if (searchElements.size > 1) {
                    KotlinLogger.logWarning("There are more than one elements to search: $searchElements")
                }

                // We assume that there is only one search element, it could be IJavaElement or KtElement
                val searchElement = searchElements.first()
                val searchResult = searchTextOccurrences(searchElement, kotlinFiles) ?: return
                if (monitor?.isCanceled == true) return

                val elements = obtainElements(searchResult as FileSearchResult, kotlinFiles).flatMap {
                    val tempImportAlias = it.getParentOfType<KtImportDirective>(false)?.alias

                    if (tempImportAlias != null) {
                        val tempEclipseFile = KotlinPsiManager.getEclipseFile(tempImportAlias.containingKtFile)!!
                        val tempResult = searchTextOccurrences(
                            SearchElement.KotlinSearchElement(tempImportAlias),
                            listOf(tempEclipseFile)
                        )
                        return@flatMap obtainElements(tempResult as FileSearchResult, listOf(tempEclipseFile)) + it
                    }

                    listOf(it)
                }

                if (monitor?.isCanceled == true) return
                val matchedReferences = resolveElementsAndMatch(elements, searchElement, querySpecification)
                if (monitor?.isCanceled == true) return
                matchedReferences.forEach { ktElement ->
                    val tempCreator = KotlinElementMatchCreator.getMatchCreator(querySpecification)
                    tempCreator.createMatch(ktElement)?.let { tempRequestor.reportMatch(it) }
                }
            }

            override fun handleException(exception: Throwable) {
                KotlinLogger.logError(exception)
            }
        })
    }

    override fun estimateTicks(specification: QuerySpecification): Int = 500

    override fun getUIParticipant() = KotlinReferenceMatchPresentation()

    private fun runCompositeSearch(
        elements: List<SearchElement>, requestor: ISearchRequestor, originSpecification: QuerySpecification,
        monitor: IProgressMonitor?
    ) {

        fun reportMatch(match: Match) {
            val tempElement = match.element
            if (tempElement is IJavaElement && EclipseJavaElementUtil.isFromKotlinBinFolder(tempElement)) {
                return
            }
            requestor.reportMatch(match)
        }

        fun reportSearchResults(result: AbstractJavaSearchResult) {
            for (searchElement in result.elements) {
                result.getMatches(searchElement).forEach { match ->
                    reportMatch(match)
                }
            }
        }

        val specifications = elements.map { searchElement ->
            when (searchElement) {
                is SearchElement.JavaSearchElement ->
                    ElementQuerySpecification(
                        searchElement.javaElement,
                        originSpecification.limitTo,
                        originSpecification.scope,
                        originSpecification.scopeDescription
                    )

                is SearchElement.KotlinSearchElement ->
                    KotlinOnlyQuerySpecification(
                        searchElement.kotlinElement,
                        originSpecification.getFilesInScope(),
                        originSpecification.limitTo,
                        originSpecification.scopeDescription
                    )
            }
        }

        for (specification in specifications) {
            if (specification is KotlinScoped) {
                search(requestor, specification, monitor)
            } else if (specification is ElementQuerySpecification && specification.limitTo == KotlinFindImplementationsInProjectAction.IMPLEMENTORS_LIMIT_TO && specification.element is IType) {
                //We need this workaround to find java subclasses!
                val tempElement = specification.element as IType
                val tempSubTypes = tempElement.newTypeHierarchy(monitor).getAllSubtypes(tempElement)
                for (tempMatch in tempSubTypes.map { Match(it, it.nameRange.offset, it.nameRange.length) }) {
                    reportMatch(tempMatch)
                }
            } else {
                val searchQuery = JavaSearchQuery(specification)
                searchQuery.run(monitor)
                reportSearchResults(searchQuery.searchResult as AbstractJavaSearchResult)
            }
        }
    }

    sealed class SearchElement private constructor() {
        abstract fun getSearchText(): String?

        class JavaSearchElement(val javaElement: IJavaElement) : SearchElement() {
            override fun getSearchText(): String = javaElement.elementName
        }

        class KotlinSearchElement(val kotlinElement: KtElement) : SearchElement() {
            override fun getSearchText(): String? = kotlinElement.name
        }
    }


    private fun getSearchElements(querySpecification: QuerySpecification): List<SearchElement> {
        fun obtainSearchElements(sourceElements: List<SourceElement>): List<SearchElement> {
            val (javaElements, kotlinElements) = getJavaAndKotlinElements(sourceElements)
            return javaElements.map { SearchElement.JavaSearchElement(it) } +
                    kotlinElements.map { SearchElement.KotlinSearchElement(it) }

        }

        return when (querySpecification) {
            is ElementQuerySpecification -> listOf(SearchElement.JavaSearchElement(querySpecification.element))
            is KotlinOnlyQuerySpecification -> listOf(SearchElement.KotlinSearchElement(querySpecification.kotlinElement))
            is KotlinAndJavaSearchable -> obtainSearchElements(querySpecification.sourceElements)
            else -> emptyList()
        }
    }

    private fun searchTextOccurrences(searchElement: SearchElement, filesScope: List<IFile>): ISearchResult? {
        var searchText = searchElement.getSearchText() ?: return null
        var asRegex = false

        if (searchElement is SearchElement.KotlinSearchElement) {
            if (searchElement.kotlinElement is KtFunction) {
                //Either it has an operator keyword directly, or it overrides something. In this case we could look in the overridden element, or we could just try to search for it!
                if (searchElement.kotlinElement.hasModifier(KtTokens.OPERATOR_KEYWORD)) {
                    val pair = getSearchTextAsRegex(searchText)
                    asRegex = pair.first
                    searchText = pair.second
                } else if (searchElement.kotlinElement.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
                    val tempDescriptor = searchElement.kotlinElement.resolveToDescriptorIfAny() as? FunctionDescriptor
                    if (tempDescriptor?.isOperator == true) {
                        val pair = getSearchTextAsRegex(searchText)
                        asRegex = pair.first
                        searchText = pair.second
                    }
                }
            }
        }

        val scope = FileTextSearchScope.newSearchScope(filesScope.toTypedArray(), null as Array<String?>?, false)

        val query = DefaultTextSearchQueryProvider().createQuery(object : TextSearchInput() {
            override fun isWholeWordSearch(): Boolean = !asRegex

            override fun getSearchText(): String = searchText

            override fun isCaseSensitiveSearch(): Boolean = true

            override fun isRegExSearch(): Boolean = asRegex

            override fun getScope(): FileTextSearchScope = scope
        })

        query.run(null)

        return query.searchResult
    }

    private fun getSearchTextAsRegex(
        searchText: String
    ): Pair<Boolean, String> {
        val tempOperationSymbol =
            (OperatorConventions.getOperationSymbolForName(Name.identifier(searchText)) as? KtSingleValueToken)?.value?.let { "\\Q$it\\E" }
                ?: when (searchText) {
                    "get" -> "\\[.*?]"
                    "set" -> "\\[.*?]\\s*?="
                    "invoke" -> "\\(.*?\\)"
                    "contains" -> "in|!in"
                    "getValue", "setValue", "provideDelegate" -> "by"
                    else -> null
                }
        if (tempOperationSymbol != null) {
            return Pair(true, "(\\b$searchText\\b|$tempOperationSymbol)")
        }
        return Pair(false, searchText)
    }

    private fun resolveElementsAndMatch(
        elements: List<KtElement>, searchElement: SearchElement,
        querySpecification: QuerySpecification
    ): List<KtElement> {
        val beforeResolveFilters = getBeforeResolveFilters(querySpecification)
        val afterResolveFilters = getAfterResolveFilters(querySpecification)
        val mapper = SearchParentObjectMapper.getMapper(querySpecification)

        // This is important for optimization: 
        // we will consequentially cache files one by one which do contain these references
        val sortedByFileNameElements = elements.sortedBy { it.containingKtFile.name }

        return sortedByFileNameElements.flatMap { element ->
            val tempElement = findApplicableElement(element, beforeResolveFilters, mapper) ?: return@flatMap emptyList()

            val sourceElements = tempElement.resolveToSourceDeclaration()
            if (sourceElements.isEmpty()) return@flatMap emptyList()

            val additionalElements = getContainingClassOrObjectForConstructor(sourceElements)

            if (afterResolveFilters.all { it.isApplicable(sourceElements, searchElement) } ||
                afterResolveFilters.all { it.isApplicable(additionalElements, searchElement) }) {
                return@flatMap listOf(tempElement)
            }
            emptyList()
        }
    }

    private fun findApplicableElement(
        element: KtElement, beforeResolveFilters: List<SearchFilter>, mapper: SearchParentObjectMapper
    ): KtElement? {
        if (beforeResolveFilters.all { it.isApplicable(element) }) return element
        return mapper.map(element)?.takeIf { refExp ->
            beforeResolveFilters.all { it.isApplicable(refExp) }
        }
    }

    private fun obtainElements(searchResult: FileSearchResult, files: List<IFile>): List<KtElement> {
        val elements = ArrayList<KtElement>()
        for (file in files) {
            val matches = searchResult.getMatches(file)
            val jetFile = KotlinPsiManager.getParsedFile(file)
            val document = EditorUtil.getDocument(file)

            matches
                .map { match ->
                    val element = jetFile.findElementByDocumentOffset(match.offset, document)
                    element?.let { PsiTreeUtil.getNonStrictParentOfType(it, KtElement::class.java) }
                }
                .filterNotNullTo(elements)
        }

        return elements
    }

    private fun getKotlinFilesByScope(querySpecification: QuerySpecification): List<IFile> {
        return when (querySpecification) {
            is ElementQuerySpecification,
            is KotlinJavaQuerySpecification -> querySpecification.scope.getKotlinFiles()
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

fun getJavaAndKotlinElements(
    sourceElements: List<SourceElement>
): Pair<List<IJavaElement>, List<KtElement>> {
    val javaElements = sourceElementsToLightElements(sourceElements)
    val kotlinElements = sourceElementsToKotlinElements(sourceElements)
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

class KotlinElementMatch(val jetElement: KtElement, val label: String, val identifier: PsiElement) :
    Match(KotlinAdaptableElement(jetElement, label), identifier.textOffset, identifier.textLength)

class KotlinAdaptableElement(val jetElement: KtElement, val label: String) : IAdaptable {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getAdapter(adapter: Class<T>?): T? {
        return when {
            IResource::class.java == adapter ->
                KotlinPsiManager.getEclipseFile(jetElement.containingKtFile) as T
            IWorkbenchAdapter::class.java == adapter ->
                object : IWorkbenchAdapter {
                    override fun getChildren(p0: Any?): Array<Any> = emptyArray()

                    override fun getImageDescriptor(p0: Any?): ImageDescriptor? = null

                    override fun getLabel(p0: Any?): String = label

                    override fun getParent(p0: Any?): Any? =
                        KotlinPsiManager.getEclipseFile(jetElement.containingKtFile)

                } as T
            else -> null
        }
    }
}
