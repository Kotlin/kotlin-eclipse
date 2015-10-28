package org.jetbrains.kotlin.ui.commands.findReferences

import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.ui.search.PatternQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchScope
import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.core.runtime.IPath
import org.jetbrains.kotlin.psi.JetFile
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.descriptors.SourceElement

class KotlinJavaQuerySpecification(
        val sourceElements: List<SourceElement>,
        limitTo: Int,
        searchScope: IJavaSearchScope,
        description: String) : KotlinDummyQuerySpecification(searchScope, description, limitTo)

class KotlinScopedQuerySpecification(
        val sourceElements: List<SourceElement>,
        val searchScope: List<IFile>,
        limitTo: Int,
        description: String) : KotlinDummyQuerySpecification(EmptyJavaSearchScope, description, limitTo)

class KotlinOnlyQuerySpecification(
        val kotlinElement: JetElement,
        val searchScope: List<IFile>,
        limitTo: Int,
        description: String) : KotlinDummyQuerySpecification(EmptyJavaSearchScope, description, limitTo)

// After passing this query specification to java, it will try to find some usages and to ensure that nothing will found
// before KotlinQueryParticipant here is using dummy element '------------'
abstract class KotlinDummyQuerySpecification(
        searchScope: IJavaSearchScope, 
        description: String, 
        limitTo: Int = IJavaSearchConstants.REFERENCES) : PatternQuerySpecification(
            "Kotlin Find References", 
            IJavaSearchConstants.CLASS, 
            true, 
            limitTo, 
            searchScope, 
            description)

object EmptyJavaSearchScope : IJavaSearchScope {
    override fun setIncludesClasspaths(includesClasspaths: Boolean) {
    }
    
    override fun setIncludesBinaries(includesBinaries: Boolean) {
    }
    
    override fun enclosingProjectsAndJars(): Array<out IPath> {
        val base = ResourcesPlugin.getWorkspace().getRoot().getLocation()
        return ResourcesPlugin.getWorkspace().getRoot().getProjects()
                .map { it.getLocation().makeRelativeTo(base) }
                .toTypedArray()
    }
    
    override fun includesBinaries(): Boolean = false
    
    override fun includesClasspaths(): Boolean = false
    
    override fun encloses(resourcePath: String?): Boolean = false
    
    override fun encloses(element: IJavaElement?): Boolean = false
}