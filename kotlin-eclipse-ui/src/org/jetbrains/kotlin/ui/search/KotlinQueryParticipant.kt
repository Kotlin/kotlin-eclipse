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
import org.jetbrains.kotlin.core.references.resolveToLightElements 
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

public class KotlinQueryParticipant : IQueryParticipant {
    override public fun search(requestor: ISearchRequestor, querySpecification: QuerySpecification, monitor: IProgressMonitor) {
        if (querySpecification !is ElementQuerySpecification) return
        
        val element = querySpecification.getElement()
        
        val files = getKotlinFilesByScope(querySpecification)
        val searchResult = searchTextOccurrences(element, files)
        if (searchResult == null) return
        
        val references = obtainReferences(searchResult as FileSearchResult, files)
        val matchedReferences = resolveReferencesAndMatch(references, element)
        
        matchedReferences.forEach { requestor.reportMatch(KotlinElementMatch(it.expression)) }
    }
    
    override public fun estimateTicks(specification: QuerySpecification): Int = 500
    
    override public fun getUIParticipant(): IMatchPresentation? {
        return KotlinReferenceMatchPresentation()
    }
    
    private fun searchTextOccurrences(element: IJavaElement, filesScope: List<IFile>): ISearchResult? {
        val scope = FileTextSearchScope.newSearchScope(filesScope.toTypedArray(), null, false)
        val query = FileSearchQuery(element.getElementName(), false, false, scope)
        
        query.run(null)
        
        return query.getSearchResult()
    }
    
    private fun resolveReferencesAndMatch(references: List<KotlinReference>, element: IJavaElement): List<KotlinReference> {
        val javaProject = element.getJavaProject()
        val analysisResult = KotlinAnalysisProjectCache.getInstance(javaProject).getAnalysisResult()
        
        return references.filter {
            it.resolveToLightElements(analysisResult.bindingContext, javaProject).any {
                it == element
            }
        }
    }
    
    private fun obtainReferences(searchResult: FileSearchResult, files: List<IFile>): List<KotlinReference> {
        val references = ArrayList<KotlinReference>()
        for (file in files) {
            val matches = searchResult.getMatches(file)
            val jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file)
            val document = EditorUtil.getDocument(file)
            
            matches
            	.map { jetFile.findElementByDocumentOffset(it.getOffset(), document) }
        		.mapNotNull { getReferenceExpression(it) }
        		.mapNotNullTo(references) { createReference(it) }
        }
        
        return references
    }
    
    private fun getKotlinFilesByScope(querySpecification: QuerySpecification): List<IFile> {
        return querySpecification.getScope().enclosingProjectsAndJars()
        		.map { JavaModel.getTarget(it, true) }
        		.filterIsInstance(javaClass<IProject>())
        		.flatMap { KotlinPsiManager.INSTANCE.getFilesByProject(it) }
    }
}

public class KotlinElementMatch(val jetElement: JetElement) : Match(jetElement, jetElement.getTextOffset(), jetElement.getTextOffset())