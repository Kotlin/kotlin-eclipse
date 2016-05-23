package com.intellij.formatting

import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormattingDocumentModelImpl
import org.jetbrains.kotlin.psi.KtFile

fun test(containingFile: KtFile, rootBlock: Block, settings: CodeStyleSettings) {
    val formattingDocumentModel =
       FormattingDocumentModelImpl(DocumentImpl(containingFile.getViewProvider().getContents(), true), containingFile);
    
    FormattingModelProvider.createFormattingModelForPsiFile(containingFile, rootBlock, settings)
}