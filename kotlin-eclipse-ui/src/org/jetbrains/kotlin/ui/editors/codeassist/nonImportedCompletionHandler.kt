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

fun lookupNonImportedTypes(
        simpleNameExpression: KtSimpleNameExpression,
        identifierPart: String,
        ktFile: KtFile,
        javaProject: IJavaProject): List<TypeNameMatch> {
    val callTypeAndReceiver = CallTypeAndReceiver.detect(simpleNameExpression)
     
    if (callTypeAndReceiver !is CallTypeAndReceiver.TYPE || callTypeAndReceiver !is CallTypeAndReceiver.DEFAULT) {
        return emptyList()
    }
    
    val importsSet = ktFile.getImportDirectives()
            .mapNotNull { it.getImportedFqName()?.asString() }
            .toSet()
    
    // TODO: exclude variants by callType.descriptorKind
    return searchFor(identifierPart, javaProject)
            .filter { it.fullyQualifiedName !in importsSet && it.packageName !in importsSet }
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
    
    val javaProjectSearchScope = JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(javaProject, true)
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