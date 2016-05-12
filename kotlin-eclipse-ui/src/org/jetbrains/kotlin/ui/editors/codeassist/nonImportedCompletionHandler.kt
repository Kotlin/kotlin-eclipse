package org.jetbrains.kotlin.ui.editors.codeassist

import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.eclipse.jdt.core.search.TypeNameMatchRequestor
import org.eclipse.jdt.core.search.TypeNameMatch
import org.eclipse.jdt.core.Flags
import org.eclipse.jdt.core.search.SearchEngine
import org.eclipse.jdt.core.search.SearchPattern
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.eclipse.jdt.core.JavaCore

fun lookupNonImportedTypes(
        simpleNameExpression: KtSimpleNameExpression,
        identifierPart: String,
        ktFile: KtFile,
        javaProject: IJavaProject): List<TypeNameMatch> {
    val callTypeAndReceiver = CallTypeAndReceiver.detect(simpleNameExpression)
     
    if ((callTypeAndReceiver !is CallTypeAndReceiver.TYPE && 
        callTypeAndReceiver !is CallTypeAndReceiver.DEFAULT) ||
        callTypeAndReceiver.receiver != null) {
        return emptyList()
    }
    
    val importsSet = ktFile.getImportDirectives()
            .mapNotNull { it.getImportedFqName()?.asString() }
            .toSet()
    
    val originPackage = ktFile.packageFqName.asString()
    
    // TODO: exclude variants by callType.descriptorKind
    return searchFor(identifierPart, javaProject)
            .filter {
                it.fullyQualifiedName !in importsSet &&
                it.packageName !in importsSet &&
                it.packageName != originPackage
            }
}

private fun searchFor(identifierPart: String, javaProject: IJavaProject): List<TypeNameMatch> {
    val foundTypes = arrayListOf<TypeNameMatch>()
    val collector = object : TypeNameMatchRequestor() {
        override fun acceptTypeNameMatch(match: TypeNameMatch) {
            val type = match.type
            if (Flags.isPublic(type.flags)) {
                foundTypes.add(match)
            }
        }
    }
    
    val searchEngine = SearchEngine()
    
    val dependencyProjects = arrayListOf<IJavaProject>().apply {
        addAll(ProjectUtils.getDependencyProjects(javaProject).map { JavaCore.create(it) })
        add(javaProject)
    }
    
    val javaProjectSearchScope = JavaSearchScopeFactory.getInstance().createJavaSearchScope(dependencyProjects.toTypedArray(), true)
    searchEngine.searchAllTypeNames(null, 
                SearchPattern.R_EXACT_MATCH, 
                identifierPart.toCharArray(), 
                SearchPattern.R_CAMELCASE_MATCH, 
                IJavaSearchConstants.TYPE, 
                javaProjectSearchScope, 
                collector,
                IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH, 
                null)
    
    return foundTypes
}