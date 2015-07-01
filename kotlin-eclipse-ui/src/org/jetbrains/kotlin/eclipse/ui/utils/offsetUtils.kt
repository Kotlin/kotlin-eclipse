package org.jetbrains.kotlin.eclipse.ui.utils

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument

fun PsiElement.getEndLfOffset(document: IDocument): Int {
	return LineEndUtil.convertLfToDocumentOffset(this.getContainingFile().getText(), this.getTextRange().getEndOffset(), document)
}