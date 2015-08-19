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
import org.jetbrains.kotlin.core.references.resolveToSourceElements 
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
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinQueryPatternSpecification
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.core.model.sourceElementsToLightElements
import org.eclipse.jface.util.SafeRunnable
import org.eclipse.core.runtime.ISafeRunnable
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinLightElementsQuerySpecification
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery
import org.eclipse.jdt.internal.ui.search.AbstractJavaSearchResult

public class KotlinQueryParticipant : IQueryParticipant {
    override public fun search(requestor: ISearchRequestor, querySpecification: QuerySpecification, monitor: IProgressMonitor) {
        val files = getKotlinFilesByScope(querySpecification)
        if (files.isEmpty()) return
        
        if (querySpecification is KotlinLightElementsQuerySpecification) {
            runCompositeSearch(requestor, querySpecification, monitor)
            return
        }
        
        SafeRunnable.run(object : ISafeRunnable {
            override fun run() {
                val searchResult = searchTextOccurrences(querySpecification, files)
                if (searchResult == null) return
                
                val references = obtainReferences(searchResult as FileSearchResult, files)
                val matchedReferences = resolveReferencesAndMatch(references, querySpecification)
                
                matchedReferences.forEach { requestor.reportMatch(KotlinElementMatch(it.expression)) }
            }
            
            override fun handleException(exception: Throwable) {
                KotlinLogger.logError(exception)
            }
        })
    }
    
    override public fun estimateTicks(specification: QuerySpecification): Int = 500
    
    override public fun getUIParticipant() = KotlinReferenceMatchPresentation()
    
    private fun runCompositeSearch(requestor: ISearchRequestor, specification: KotlinLightElementsQuerySpecification, 
            monitor: IProgressMonitor) {
        
        fun reportSearchResults(result: AbstractJavaSearchResult) {
            for (searchElement in result.getElements()) {
                result.getMatches(searchElement).forEach { requestor.reportMatch(it) }
            }
        }
        
        specification.lightElements
            .map { ElementQuerySpecification(it, specification.getLimitTo(), specification.getScope(), specification.getScopeDescription()) }
            .forEach { 
                val searchQuery = JavaSearchQuery(it)
                searchQuery.run(monitor)
                reportSearchResults(searchQuery.getSearchResult() as AbstractJavaSearchResult)
            }
    }
    
    private fun searchTextOccurrences(querySpecification: QuerySpecification, filesScope: List<IFile>): ISearchResult? {
        val scope = FileTextSearchScope.newSearchScope(filesScope.toTypedArray(), null, false)
        val searchText = when (querySpecification) {
            is KotlinQueryPatternSpecification -> buildOrPattern(querySpecification.jetElements)
            is ElementQuerySpecification -> querySpecification.getElement().getElementName()
            else -> return null
        }
        
        val query = FileSearchQuery(searchText, false, false, scope)
        
        query.run(null)
        
        return query.getSearchResult()
    }
    
    private fun resolveReferencesAndMatch(references: List<KotlinReference>, querySpecification: QuerySpecification): List<KotlinReference> {
        val isApplicable: (List<SourceElement>, IJavaProject) -> Boolean = when (querySpecification) {
            is KotlinQueryPatternSpecification -> { elements, _ ->
                elements.any { (it as? KotlinSourceElement)?.psi in querySpecification.jetElements }
            }
            
            is ElementQuerySpecification -> { elements, project ->
                sourceElementsToLightElements(elements, project).any { referenceFilter(it, querySpecification.getElement()) }
            }
            
            else -> throw IllegalArgumentException("$querySpecification is not supported to resolve and search references")
        }
        
        return references.filter { reference ->
            val javaProject = KotlinPsiManager.getJavaProject(reference.expression)
            return@filter if (javaProject != null) {
                    val analysisResult = KotlinAnalysisProjectCache.getAnalysisResult(javaProject)
                    val sourceElements = reference.resolveToSourceElements(analysisResult.bindingContext)
                    isApplicable(sourceElements, javaProject)
                } else {
                    false
                }
        }
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
    
    private fun buildOrPattern(elements: List<JetElement>): String = elements.joinToString("|") { it.getName()!! }
}

public class KotlinElementMatch(val jetElement: JetElement) : Match(KotlinAdaptableElement(jetElement), jetElement.getTextOffset(), 
        jetElement.getTextOffset())

class KotlinAdaptableElement(val jetElement: JetElement): IAdaptable {
    override fun getAdapter(adapter: Class<*>?): Any? {
        return when {
            javaClass<IResource>() == adapter ->  KotlinPsiManager.getEclispeFile(jetElement.getContainingJetFile())
            else -> null
        }
    }
}