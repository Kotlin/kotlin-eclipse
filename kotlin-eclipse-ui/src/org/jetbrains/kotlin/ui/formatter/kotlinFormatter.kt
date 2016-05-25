package org.jetbrains.kotlin.ui.formatter

import com.intellij.formatting.Block
import com.intellij.formatting.FormatTextRanges
import com.intellij.formatting.FormatterImpl
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

@Volatile var settings: CodeStyleSettings = CodeStyleSettings(true)

@JvmOverloads
fun formatCode(source: String, javaProject: IJavaProject, lineSeparator: String, initialIndent: Int = 0): String {
    return formatCode(source, createPsiFactory(javaProject), lineSeparator, initialIndent)
}

@JvmOverloads
fun formatCode(source: String, psiFactory: KtPsiFactory, lineSeparator: String, initialIndent: Int = 0): String {
    val firstRun = KotlinFormatter(source, psiFactory, initialIndent, lineSeparator).formatCode()
    return KotlinFormatter(firstRun, psiFactory, initialIndent, lineSeparator).formatCode()
}

fun reformatAll(containingFile: KtFile, rootBlock: Block, settings: CodeStyleSettings, document: IDocument) {
        val formattingDocumentModel =
            EclipseFormattingModel(DocumentImpl(containingFile.getViewProvider().getContents(), true), containingFile, settings)

    val formattingModel = EclipseDocumentFormattingModel(containingFile, rootBlock, formattingDocumentModel, document, settings)
            
    val ranges = FormatTextRanges(containingFile.getTextRange(), true)
    FormatterImpl().format(formattingModel, settings, settings.indentOptions, ranges, false);
}

val NULL_ALIGNMENT_STRATEGY = NodeAlignmentStrategy.fromTypes(KotlinAlignmentStrategy.wrap(null))

private class KotlinFormatter(source: String, psiFactory: KtPsiFactory, val initialIndent: Int, val lineSeparator: String) {
    
    val ktFile = createKtFile(source, psiFactory)
    
    val sourceDocument = Document(source)
    
    fun formatCode(): String {
        return sourceDocument.get()
    }
}

private fun createPsiFactory(javaProject: IJavaProject): KtPsiFactory {
    val environment = KotlinEnvironment.getEnvironment(javaProject)
    val ideaProject = environment.getProject()
    return KtPsiFactory(ideaProject)
}

private fun createKtFile(source: String, psiFactory: KtPsiFactory): KtFile {
    return psiFactory.createFile(StringUtil.convertLineSeparators(source))
}

private fun createWhitespaces(countSpaces: Int) = IndenterUtil.SPACE_STRING.repeat(countSpaces)