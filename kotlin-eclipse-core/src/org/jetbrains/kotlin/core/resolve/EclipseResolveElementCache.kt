package org.jetbrains.kotlin.core.resolve

import org.jetbrains.kotlin.idea.analysis.ElementResolver
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.idea.analysis.CodeFragmentAnalyzer

class EclipseResolveElementCache(private val codeFragmentAnalyzer: CodeFragmentAnalyzer) : ElementResolver {
    override fun resolveToElements(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        return BindingContext.EMPTY
    }
}