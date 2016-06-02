package org.jetbrains.kotlin.ui.editors.codeassist

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiType
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.filters.position.PositionElementFilter

class TextFilter(val value: String) : ElementFilter {
    override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
        if (element == null) return false
        return getTextByElement(element) == value
    }

    override fun isClassAcceptable(hintClass: Class<*>): Boolean = true
    
    private fun getTextByElement(element: Any): String? {
        return when (element) {
            is PsiType -> element.presentableText
            is PsiNamedElement -> element.name
            is PsiElement -> element.text
            else -> null
        }
    }
}

class LeftNeighbour(filter: ElementFilter) : PositionElementFilter() {
    init {
        setFilter(filter)
    }
    
    override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
        if (element !is PsiElement) return false
        
        val previous = FilterPositionUtil.searchNonSpaceNonCommentBack(element)
        return if (previous != null) getFilter().isAcceptable(previous, context) else false
    }
}