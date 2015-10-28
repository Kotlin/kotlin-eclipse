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
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import java.util.ArrayList
import org.jetbrains.kotlin.core.references.KotlinReference
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache
import org.eclipse.search.ui.text.Match
import org.eclipse.jface.viewers.ILabelProvider
import org.jetbrains.kotlin.ui.editors.outline.PsiLabelProvider
import org.eclipse.jface.viewers.LabelProvider
import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.jdt.internal.core.JavaModel
import org.eclipse.core.resources.IProject
import org.jetbrains.kotlin.core.references.createReference
import org.eclipse.core.runtime.IAdaptable
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetClassOrObject
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
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.JetObjectDeclarationName
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinScopedQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinJavaQuerySpecification
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinOnlyQuerySpecification
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinAndJavaSearchable
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinScoped

public class KotlinQueryParticipant : IQueryParticipant {
    override public fun search(requestor: ISearchRequestor, querySpecification: QuerySpecification, monitor: IProgressMonitor?) {
        SafeRunnable.run(object : ISafeRunnable {
            override fun run() {
                val searchElements = getSearchElements(querySpecification)
                if (searchElements.isEmpty()) return
                
                if (querySpecification !is ElementQuerySpecification && querySpecification !is KotlinScoped) {
                    runCompositeSearch(searchElements, requestor, querySpecification, monitor)
                    return
                }
                
                val files = getKotlinFilesByScope(querySpecification)
                if (files.isEmpty()) return
                
                val searchElement = searchElements.first()
                val searchResult = searchTextOccurrences(searchElement, files)
                if (searchResult == null) return
                
                val elements = obtainElements(searchResult as FileSearchResult, files)
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
    
    private fun runCompositeSearch(elements: List<SearchElement>, requestor: ISearchRequestor, specification: QuerySpecification, 
            monitor: IProgressMonitor?) {
        
        fun reportSearchResults(result: AbstractJavaSearchResult) {
            for (searchElement in result.getElements()) {
                result.getMatches(searchElement).forEach { requestor.reportMatch(it) }
            }
        }
        
        val specifications = 
            elements.getElements<IJavaElement>().map { 
                ElementQuerySpecification(
                    it, 
                    specification.getLimitTo(), 
                    specification.getScope(), 
                    specification.getScopeDescription()) 
            } + elements.getElements<JetElement>().map { 
                KotlinOnlyQuerySpecification(
                    it,
                    specification.getScope().getKotlinFiles(), 
                    specification.getLimitTo(), 
                    specification.getScopeDescription())
            }
        
        
        specifications.forEach { 
            val searchQuery = JavaSearchQuery(it)
            searchQuery.run(monitor)
            reportSearchResults(searchQuery.getSearchResult() as AbstractJavaSearchResult)
        }
    }
    
    class SearchElement private constructor(private val javaElement: IJavaElement?, private val kotlinElement: JetElement?) {
        constructor(element: IJavaElement) : this(element, null)
        constructor(element: JetElement) : this(null, element)
        
        fun getElement(): Any = javaElement ?: kotlinElement!!
        
        fun getSearchText(): String? {
            return if (javaElement != null) {
                javaElement.getElementName()
            } else if (kotlinElement != null) {
                kotlinElement.getName()
            } else {
                null
            }
        }
    }
    
    inline private fun <reified T> List<SearchElement>.getElements(): List<T> = map { it.getElement() }.filterIsInstance()
    
    private fun getSearchElements(querySpecification: QuerySpecification): List<SearchElement> {
        fun obtainSearchElements(sourceElements: List<SourceElement>): List<SearchElement> {
            val (javaElements, kotlinElements) = getJavaAndKotlinElements(sourceElements)
            return javaElements.map(::SearchElement) + kotlinElements.map(::SearchElement)
        }
        
        return when (querySpecification) {
            is ElementQuerySpecification -> listOf(SearchElement(querySpecification.getElement()))
            is KotlinOnlyQuerySpecification -> listOf(SearchElement(querySpecification.kotlinElement))
            is KotlinAndJavaSearchable -> obtainSearchElements(querySpecification.sourceElements)
            else -> emptyList()
        }
    }
    
    private fun searchTextOccurrences(searchElement: SearchElement, filesScope: List<IFile>): ISearchResult? {
        val scope = FileTextSearchScope.newSearchScope(filesScope.toTypedArray(), null, false)
        val searchText = searchElement.getSearchText()
        if (searchText == null) return null
        
        val query = FileSearchQuery(searchText, false, true, true, scope)
        
        query.run(null)
        
        return query.getSearchResult()
    }
    
    private fun resolveElementsAndMatch(elements: List<JetElement>, searchElement: SearchElement, 
            querySpecification: QuerySpecification): List<JetElement> {
        val beforeResolveFilters = getBeforeResolveFilters(querySpecification)
        val afterResolveFilters = getAfterResolveFilters()
        
        // This is important for optimization: 
        // we will consequentially cache files one by one which are containing these references
        val sortedByFileNameElements = elements.sortedBy { it.getContainingJetFile().getName() }
        
        return sortedByFileNameElements.filter { element ->
            val beforeResolveCheck = beforeResolveFilters.all { it.isApplicable(element) }
            if (!beforeResolveCheck) return@filter false
            
            val javaProject = KotlinPsiManager.getJavaProject(element)
            if (javaProject == null) return@filter false
            
            val sourceElements = element.resolveToSourceDeclaration(javaProject)
            if (sourceElements.isEmpty()) return@filter false
            
            return@filter afterResolveFilters.all { it.isApplicable(sourceElements, searchElement) }
        }
    }
    
    private fun obtainElements(searchResult: FileSearchResult, files: List<IFile>): List<JetElement> {
        val elements = ArrayList<JetElement>()
        for (file in files) {
            val matches = searchResult.getMatches(file)
            val jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file)
            val document = EditorUtil.getDocument(file)
            
            matches
                .map { jetFile.findElementByDocumentOffset(it.getOffset(), document) }
                .mapNotNull { PsiTreeUtil.getNonStrictParentOfType(it, JetElement::class.java) }
                .filterNotNullTo(elements)
        }
        
        return elements
    }
    
    private fun getKotlinFilesByScope(querySpecification: QuerySpecification): List<IFile> {
        return when (querySpecification) {
            is ElementQuerySpecification -> querySpecification.getScope().getKotlinFiles()
            is KotlinJavaQuerySpecification -> querySpecification.getScope().getKotlinFiles()
            is KotlinScoped -> querySpecification.searchScope
            else -> emptyList()
        }
    }
}

internal fun getJavaAndKotlinElements(sourceElements: List<SourceElement>): Pair<List<IJavaElement>, List<JetElement>> {
    val javaElements = sourceElementsToLightElements(sourceElements)
    val kotlinElements = sourceElementsToKotlinElements(sourceElements).filterNot { kotlinElement -> 
        javaElements.any { it.getElementName() == kotlinElement.getName() }
    }
    
    return Pair(javaElements, kotlinElements)
}

private fun sourceElementsToKotlinElements(sourceElements: List<SourceElement>): List<JetElement> {
    return sourceElements
            .filterIsInstance(KotlinSourceElement::class.java)
            .map { it.psi }
}

fun IJavaSearchScope.getKotlinFiles(): List<IFile> {
    return enclosingProjectsAndJars()
            .map { JavaModel.getTarget(it, true) }
            .filterIsInstance(IProject::class.java)
            .flatMap { KotlinPsiManager.INSTANCE.getFilesByProject(it) }
}

public class KotlinElementMatch(val jetElement: JetElement) : Match(KotlinAdaptableElement(jetElement), jetElement.getTextOffset(), 
        jetElement.getTextOffset())

class KotlinAdaptableElement(val jetElement: JetElement): IAdaptable {
    override fun getAdapter(adapter: Class<*>?): Any? {
        return when {
            IResource::class.java == adapter ->  KotlinPsiManager.getEclispeFile(jetElement.getContainingJetFile())
            else -> null
        }
    }
}