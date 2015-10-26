package org.jetbrains.kotlin.ui.commands.findReferences

import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.ui.search.PatternQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.core.runtime.IPath
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.KotlinOnlyScopeDeclaration
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.JavaAndKotlinScopeDeclaration
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.NoDeclaration

// This pattern is using to run composite search which is described in KotlinQueryParticipant.
class KotlinCompositeQuerySpecification(
        val lightElements: List<IJavaElement>, 
        val jetElement: JetElement?, 
        searchScope: IJavaSearchScope, 
        description: String) : KotlinDummyQuerySpecification(searchScope, description)

class KotlinQuerySpecification(
        val declaration: VisibilityScopeDeclaration,
        val searchScope: List<JetFile>,
        limitTo: Int,
        description: String) : KotlinDummyQuerySpecification(EmptyJavaSearchScope, description, limitTo), KotlinTextSearchable {
    override fun getSearchText(): String {
        return when (declaration) {
            is KotlinOnlyScopeDeclaration -> declaration.getSearchText()
            is JavaAndKotlinScopeDeclaration -> declaration.javaElements.first().getElementName()
            is NoDeclaration -> throw IllegalStateException("Cannot get search text for $declaration")
        }
    }
}

// Using of this pattern assumes that elements presents only in Kotlin
class KotlinLocalQuerySpecification(
        val localDeclaration: KotlinOnlyScopeDeclaration, 
        limitTo: Int,
        description: String) : KotlinDummyQuerySpecification(EmptyJavaSearchScope, description, limitTo), KotlinTextSearchable {
    override fun getSearchText(): String {
        return localDeclaration.getSearchText()
    }
}

fun KotlinOnlyScopeDeclaration.getSearchText(): String = this.jetDeclaration.getName()!!

// After passing this query specification to java, it will try to find some usages and to ensure that nothing will found
// before KotlinQueryParticipant here is using dummy element '------------'
open class KotlinDummyQuerySpecification(searchScope: IJavaSearchScope, description: String, limitTo: Int = IJavaSearchConstants.REFERENCES) : PatternQuerySpecification(
        "Kotlin Find References", 
        IJavaSearchConstants.CLASS, 
        true, 
        limitTo, 
        searchScope, 
        description
)

interface KotlinTextSearchable {
    fun getSearchText(): String
}

object EmptyJavaSearchScope : IJavaSearchScope {
    override fun setIncludesClasspaths(includesClasspaths: Boolean) {
    }
    
    override fun setIncludesBinaries(includesBinaries: Boolean) {
    }
    
    override fun enclosingProjectsAndJars(): Array<out IPath> = emptyArray()
    
    override fun includesBinaries(): Boolean = false
    
    override fun includesClasspaths(): Boolean = false
    
    override fun encloses(resourcePath: String?): Boolean = false
    
    override fun encloses(element: IJavaElement?): Boolean = false
}