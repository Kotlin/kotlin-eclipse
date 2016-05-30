package org.jetbrains.kotlin.ui.formatter

import com.intellij.formatting.Block
import com.intellij.formatting.DependantSpacingImpl
import com.intellij.formatting.DependentSpacingRule
import com.intellij.formatting.FormatTextRanges
import com.intellij.formatting.FormatterImpl
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.util.text.CharArrayUtil
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.idea.formatter.KotlinSpacingBuilderUtil
import org.jetbrains.kotlin.idea.formatter.createSpacingBuilder
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.intellij.openapi.editor.Document as IdeaDocument
import com.intellij.psi.codeStyle.CommonCodeStyleSettings.IndentOptions
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil

@Volatile var settings: CodeStyleSettings = CodeStyleSettings(true)

fun formatCode(source: String, javaProject: IJavaProject, lineSeparator: String): String {
    return formatCode(source, createPsiFactory(javaProject), lineSeparator)
}

fun formatCode(source: String, psiFactory: KtPsiFactory, lineSeparator: String): String {
    return KotlinFormatter(source, psiFactory, lineSeparator).formatCode()
}

fun reformatAll(containingFile: KtFile, rootBlock: Block, settings: CodeStyleSettings, document: IDocument) {
    formatRange(containingFile, rootBlock, settings, document, containingFile.textRange)
}

fun formatRange(document: IDocument, range: EclipseDocumentRange, psiFactory: KtPsiFactory) {
    formatRange(document, range.toPsiRange(document), psiFactory)
}

fun formatRange(document: IDocument, range: TextRange, psiFactory: KtPsiFactory) {
    val ktFile = createKtFile(document.get(), psiFactory)
    val rootBlock = KotlinBlock(ktFile.getNode(), 
                NULL_ALIGNMENT_STRATEGY, 
                Indent.getNoneIndent(), 
                null,
                settings,
                createSpacingBuilder(settings, KotlinSpacingBuilderUtilImpl))
    
    formatRange(ktFile, rootBlock, settings, document, range)
}

private fun formatRange(
        containingFile: KtFile,
        rootBlock: Block,
        settings: CodeStyleSettings,
        document: IDocument,
        range: TextRange) {
    val formattingModel = buildModel(containingFile, rootBlock, settings, document)
            
    val ranges = FormatTextRanges(range, true)
    FormatterImpl().format(formattingModel, settings, settings.indentOptions, ranges, false)
}

fun adjustIndent(containingFile: KtFile, rootBlock: Block,
                 settings: CodeStyleSettings, offset: Int, document: IDocument) {
    val formattingModel = buildModel(containingFile, rootBlock, settings, document)
    FormatterImpl().adjustLineIndent(
            formattingModel, settings, settings.indentOptions, offset, getSignificantRange(containingFile, offset))
}

fun getMockDocument(document: IdeaDocument): IdeaDocument {
    return object : IdeaDocument by document {
    }
}

fun initializaSettings(options: IndentOptions) {
    with(options) {
        USE_TAB_CHARACTER = !IndenterUtil.isSpacesForTabs()
        INDENT_SIZE = IndenterUtil.getDefaultIndent()
        TAB_SIZE = IndenterUtil.getDefaultIndent()
    }
}

data class EclipseDocumentRange(val startOffset: Int, val endOffset: Int)

private fun EclipseDocumentRange.toPsiRange(document: IDocument): TextRange {
    val startPsiOffset = LineEndUtil.convertCrToDocumentOffset(document, startOffset)
    val endPsiOffset = LineEndUtil.convertCrToDocumentOffset(document, endOffset)
    return TextRange(startPsiOffset, endPsiOffset)
}

val NULL_ALIGNMENT_STRATEGY = NodeAlignmentStrategy.fromTypes(KotlinAlignmentStrategy.wrap(null))

private fun buildModel(
        containingFile: KtFile,
        rootBlock: Block,
        settings: CodeStyleSettings,
        document: IDocument): EclipseDocumentFormattingModel {
    initializaSettings(settings.indentOptions!!)
    val formattingDocumentModel =
            EclipseFormattingModel(DocumentImpl(containingFile.getViewProvider().getContents(), true), containingFile, settings)

    return EclipseDocumentFormattingModel(containingFile, rootBlock, formattingDocumentModel, document, settings)
}


private fun getSignificantRange(file: KtFile, offset: Int): TextRange {
    val elementAtOffset = file.findElementAt(offset)
    if (elementAtOffset == null) {
        val significantRangeStart = CharArrayUtil.shiftBackward(file.getText(), offset - 1, "\r\t ");
        return TextRange(Math.max(significantRangeStart, 0), offset);
    }

    return elementAtOffset.getTextRange()
}


private class KotlinFormatter(source: String, psiFactory: KtPsiFactory, val lineSeparator: String) {
    
    val ktFile = createKtFile(source, psiFactory)
    
    val sourceDocument = Document(source)
    
    fun formatCode(): String {
        FormatterImpl()
        val rootBlock = KotlinBlock(ktFile.getNode(), 
                NULL_ALIGNMENT_STRATEGY, 
                Indent.getNoneIndent(), 
                null,
                settings,
                createSpacingBuilder(settings, KotlinSpacingBuilderUtilImpl))
        
        reformatAll(ktFile, rootBlock, settings, sourceDocument)
        
        return sourceDocument.get()
    }
}

fun createPsiFactory(javaProject: IJavaProject): KtPsiFactory {
    val environment = KotlinEnvironment.getEnvironment(javaProject)
    val ideaProject = environment.getProject()
    return KtPsiFactory(ideaProject)
}

fun createKtFile(source: String, psiFactory: KtPsiFactory): KtFile {
    return psiFactory.createFile(StringUtil.convertLineSeparators(source))
}

private fun createWhitespaces(countSpaces: Int) = IndenterUtil.SPACE_STRING.repeat(countSpaces)

object KotlinSpacingBuilderUtilImpl : KotlinSpacingBuilderUtil {
    override fun createLineFeedDependentSpacing(minSpaces: Int,
            maxSpaces: Int,
            minimumLineFeeds: Int,
            keepLineBreaks: Boolean,
            keepBlankLines: Int,
            dependency: TextRange,
            rule: DependentSpacingRule): Spacing {
        return object : DependantSpacingImpl(minSpaces, maxSpaces, dependency, keepLineBreaks, keepBlankLines, rule) {
        }
    }
    
    override fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode? {
        return FormatterUtil.getPreviousNonWhitespaceLeaf(node)
    }
    
    override fun isWhitespaceOrEmpty(node: ASTNode?): Boolean {
        return FormatterUtil.isWhitespaceOrEmpty(node)
    }
}