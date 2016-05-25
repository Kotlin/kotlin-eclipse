package org.jetbrains.kotlin.ui.formatter

import com.intellij.formatting.Block
import com.intellij.formatting.FormatterImpl
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.util.text.CharArrayUtil
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.formatting.FormatTextRanges

fun tryAdjustIndent(containingFile: KtFile, rootBlock: Block, settings: CodeStyleSettings, offset: Int, document: IDocument) {
    val formattingDocumentModel =
            EclipseFormattingModel(DocumentImpl(containingFile.getViewProvider().getContents(), true), containingFile, settings);

    val formattingModel = EclipseBasedFormattingModel(containingFile, rootBlock, formattingDocumentModel, document)

    FormatterImpl().adjustLineIndent(
            formattingModel, settings, settings.indentOptions, offset, getSignificantRange(containingFile, offset))
}

fun reformatAll(containingFile: KtFile, rootBlock: Block, settings: CodeStyleSettings, document: IDocument) {
        val formattingDocumentModel =
            EclipseFormattingModel(DocumentImpl(containingFile.getViewProvider().getContents(), true), containingFile, settings);

    val formattingModel = EclipseDocumentFormattingModel(containingFile, rootBlock, formattingDocumentModel, document, settings)
            
    val ranges = FormatTextRanges(containingFile.getTextRange(), true)
    FormatterImpl().format(formattingModel, settings, settings.indentOptions, ranges, false);
}

fun getSignificantRange(file: KtFile, offset: Int): TextRange {
    val elementAtOffset = file.findElementAt(offset);
    if (elementAtOffset == null) {
        val significantRangeStart = CharArrayUtil.shiftBackward(file.getText(), offset - 1, "\r\t ");
        return TextRange(Math.max(significantRangeStart, 0), offset);
    }

    return elementAtOffset.getTextRange()
}