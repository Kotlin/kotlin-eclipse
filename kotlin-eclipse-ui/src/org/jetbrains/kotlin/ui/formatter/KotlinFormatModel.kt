package org.jetbrains.kotlin.ui.formatter

import com.intellij.formatting.Block
import com.intellij.formatting.EclipseBasedFormattingModel
import com.intellij.formatting.EclipseFormattingModel
import com.intellij.formatting.FormatterImpl
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.SourceTreeToPsiMap
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.util.text.CharArrayUtil

fun tryAdjustIndent(containingFile: KtFile, rootBlock: Block, settings: CodeStyleSettings, offset: Int) {
    val formattingDocumentModel =
            EclipseFormattingModel(DocumentImpl(containingFile.getViewProvider().getContents(), true), containingFile, settings);

    val formattingModel = EclipseBasedFormattingModel(containingFile, rootBlock, formattingDocumentModel)
    //    val model = DocumentBasedFormattingModel(formattingModel, document, myCodeStyleManager.getProject(), mySettings,
    //                                                   file.getFileType(), file)

    val offset1 = FormatterImpl().adjustLineIndent(
            formattingModel, settings, settings.indentOptions, offset, getSignificantRange(containingFile, offset))
    println(offset1)
}

fun getSignificantRange(file: KtFile, offset: Int): TextRange {
    val elementAtOffset = file.findElementAt(offset);
    if (elementAtOffset == null) {
        val significantRangeStart = CharArrayUtil.shiftBackward(file.getText(), offset - 1, "\r\t ");
        return TextRange(Math.max(significantRangeStart, 0), offset);
    }

    return elementAtOffset.getTextRange()
}