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
import com.intellij.openapi.util.text.StringUtil

fun lookupNonImportedTypes(
        simpleNameExpression: KtSimpleNameExpression,
        identifierPart: String,
        ktFile: KtFile,
        javaProject: IJavaProject): List<TypeNameMatch> {
    if (!identifierPart.isCapitalized()) return emptyList()
    
    val callTypeAndReceiver = CallTypeAndReceiver.detect(simpleNameExpression)
    
    val isAnnotation = callTypeAndReceiver is CallTypeAndReceiver.ANNOTATION
     
    if ((callTypeAndReceiver !is CallTypeAndReceiver.TYPE &&
            callTypeAndReceiver !is CallTypeAndReceiver.DEFAULT &&
            !isAnnotation) ||
            callTypeAndReceiver.receiver != null) {
        return emptyList()
    }
    
    val importsSet = ktFile.importDirectives
            .mapNotNull { it.importedFqName?.asString() }
            .toSet()
    
    val originPackage = ktFile.packageFqName.asString()
    
    // TODO: exclude variants by callType.descriptorKind
    return searchFor(identifierPart, javaProject, isAnnotation)
            .filter {
                it.fullyQualifiedName !in importsSet &&
                it.packageName !in importsSet &&
                it.packageName != originPackage
            }
}

private fun String.isCapitalized(): Boolean = isNotEmpty() && this[0].isUpperCase()

private fun searchFor(identifierPart: String, javaProject: IJavaProject, isAnnotation: Boolean): List<TypeNameMatch> {
    val foundTypes = arrayListOf<TypeNameMatch>()
    val collector = object : TypeNameMatchRequestor() {
        override fun acceptTypeNameMatch(match: TypeNameMatch) {
            if (Flags.isPublic(match.modifiers)) {
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
                if (isAnnotation) IJavaSearchConstants.ANNOTATION_TYPE else IJavaSearchConstants.TYPE, 
                javaProjectSearchScope, 
                collector,
                IJavaSearchConstants.FORCE_IMMEDIATE_SEARCH, 
                null)
    
    return foundTypes
}