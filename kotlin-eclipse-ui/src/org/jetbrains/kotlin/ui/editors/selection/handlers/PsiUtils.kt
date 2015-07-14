package org.jetbrains.kotlin.ui.editors.selection.handlers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange

public val PsiElement.allChildren: PsiChildRange
    get() {
        val first = getFirstChild()
        return if (first != null) PsiChildRange(first, getLastChild()) else PsiChildRange.EMPTY
    }
public val PsiElement.startOffset: Int
    get() = getTextRange().getStartOffset()

public val PsiElement.endOffset: Int
    get() = getTextRange().getEndOffset()