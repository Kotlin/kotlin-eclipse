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
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinLocalQuerySpecification
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.eclipse.jface.util.SafeRunnable
import org.eclipse.core.runtime.ISafeRunnable
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinCompositeQuerySpecification
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery
import org.eclipse.jdt.internal.ui.search.AbstractJavaSearchResult
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.JetObjectDeclarationName
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.NoDeclaration

public class KotlinQueryParticipant : IQueryParticipant {
    override public fun search(requestor: ISearchRequestor, querySpecification: QuerySpecification, monitor: IProgressMonitor?) {
        if (querySpecification is KotlinCompositeQuerySpecification) {
            runCompositeSearch(requestor, querySpecification, monitor)
            return
        }
        
        SafeRunnable.run(object : ISafeRunnable {
            override fun run() {
                val files = getKotlinFilesByScope(querySpecification)
                if (files.isEmpty()) return
                
                val searchResult = searchTextOccurrences(querySpecification, files)
                if (searchResult == null) return
                
                val elements = obtainElements(searchResult as FileSearchResult, files)
                val matchedReferences = resolveElementsAndMatch(elements, querySpecification)
                
                matchedReferences.forEach { requestor.reportMatch(KotlinElementMatch(it)) }
            }
            
            override fun handleException(exception: Throwable) {
                KotlinLogger.logError(exception)
            }
        })
    }
    
    override public fun estimateTicks(specification: QuerySpecification): Int = 500
    
    override public fun getUIParticipant() = KotlinReferenceMatchPresentation()
    
    private fun runCompositeSearch(requestor: ISearchRequestor, specification: KotlinCompositeQuerySpecification, 
            monitor: IProgressMonitor?) {
        
        fun reportSearchResults(result: AbstractJavaSearchResult) {
            for (searchElement in result.getElements()) {
                result.getMatches(searchElement).forEach { requestor.reportMatch(it) }
            }
        }
        
        val specifications = arrayListOf<QuerySpecification>()
        specification.lightElements.mapTo(specifications) { 
            ElementQuerySpecification(it, 
                specification.getLimitTo(), 
                specification.getScope(), 
                specification.getScopeDescription())
        }
        specification.jetElement?.let {
            specifications.add(KotlinLocalQuerySpecification(it, specification.getLimitTo(), 
                    specification.getScopeDescription()))
        }
        
        specifications.forEach {
            val searchQuery = JavaSearchQuery(it)
            searchQuery.run(monitor)
            reportSearchResults(searchQuery.getSearchResult() as AbstractJavaSearchResult)
        }
    }
    
    private fun searchTextOccurrences(querySpecification: QuerySpecification, filesScope: List<IFile>): ISearchResult? {
        val scope = FileTextSearchScope.newSearchScope(filesScope.toTypedArray(), null, false)
        val searchText = when (querySpecification) {
            is KotlinLocalQuerySpecification -> querySpecification.jetElement.getName()!!
            is ElementQuerySpecification -> querySpecification.getElement().getElementName()
            else -> return null
        }
        
        val query = FileSearchQuery(searchText, false, true, true, scope)
        
        query.run(null)
        
        return query.getSearchResult()
    }
    
    private fun resolveElementsAndMatch(elements: List<JetElement>, querySpecification: QuerySpecification): List<JetElement> {
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
            
            val sourceDeclaration = element.resolveToSourceDeclaration(javaProject)
            if (sourceDeclaration is NoDeclaration) return@filter false
            
            return@filter afterResolveFilters.all { it.isApplicable(sourceDeclaration, querySpecification) }
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
//        We can significantly reduce scope to one file when there are no light elements in query specification.
//        In this case search elements are not visible from Java and other Kotlin files
        return if (querySpecification is KotlinLocalQuerySpecification) {
                listOf(KotlinPsiManager.getEclispeFile(querySpecification.jetElement.getContainingJetFile())!!)
            } else {
                querySpecification.getScope().enclosingProjectsAndJars()
                    .map { JavaModel.getTarget(it, true) }
                    .filterIsInstance(IProject::class.java)
                    .flatMap { KotlinPsiManager.INSTANCE.getFilesByProject(it) }
            }
    }
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