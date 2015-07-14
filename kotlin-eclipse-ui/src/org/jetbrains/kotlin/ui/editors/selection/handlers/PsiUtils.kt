package org.jetbrains.kotlin.ui.editors.selection.handlers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

public val PsiElement.allChildren: PsiChildRange
    get() {
        val first = getFirstChild()
        return if (first != null) PsiChildRange(first, getLastChild()) else PsiChildRange.EMPTY
    }
public fun PsiElement.siblings(forward: Boolean = true, withItself: Boolean = true): Sequence<PsiElement> {
    val stepFun = if (forward) { e: PsiElement -> e.getNextSibling() } else { e: PsiElement -> e.getPrevSibling() }
    val sequence = sequence(this, stepFun)
    return if (withItself) sequence else sequence.drop(1)
}
public val PsiElement.startOffset: Int
    get() = getTextRange().getStartOffset()

public val PsiElement.endOffset: Int
    get() = getTextRange().getEndOffset()