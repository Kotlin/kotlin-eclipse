package org.jetbrains.kotlin.ui.commands.findReferences

import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.ui.search.PatternQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.core.runtime.IPath
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.JavaAndKotlinScopeDeclaration
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration.KotlinOnlyScopeDeclaration
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration
import org.jetbrains.kotlin.psi.JetFile
import org.eclipse.jdt.ui.search.ElementQuerySpecification

// This pattern is using to run composite search which is described in KotlinQueryParticipant.
class KotlinCompositeQuerySpecification(
        val javaSearchQueries: List<ElementQuerySpecification>, 
        val kotlinSearchQueries: List<KotlinQuerySpecification>, 
        description: String) : KotlinDummyQuerySpecification(EmptyJavaSearchScope, description)

// Using of this pattern assumes that elements presents only in Kotlin
class KotlinLocalQuerySpecification(
        val kotlinDeclaration: KotlinOnlyScopeDeclaration, 
        limitTo: Int,
        description: String) : KotlinDummyQuerySpecification(EmptyJavaSearchScope, description, limitTo)

class KotlinQuerySpecification(
        val sourceDeclaration: VisibilityScopeDeclaration,
        val searchScope: List<JetFile>,
        limitTo: Int) : KotlinDummyQuerySpecification(EmptyJavaSearchScope, "none", limitTo)

// After passing this query specification to java, it will try to find some usages and to ensure that nothing will found
// before KotlinQueryParticipant here is using dummy element '------------'
open class KotlinDummyQuerySpecification(
        searchScope: IJavaSearchScope, 
        description: String, 
        limitTo: Int = IJavaSearchConstants.REFERENCES) : PatternQuerySpecification(
            "Kotlin Find References", 
            IJavaSearchConstants.CLASS, 
            true, 
            limitTo, 
            searchScope, 
            description)

//inline fun <reified T : VisibilityScopeDeclaration> withElement(action: ) {
//} 

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