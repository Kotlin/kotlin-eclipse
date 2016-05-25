package org.jetbrains.kotlin.ui.formatter

import com.intellij.formatting.*
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.tree.TreeUtil
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

fun getMockDocument(document: IdeaDocument): IdeaDocument {
    return object : IdeaDocument by document {
    }
}

val NULL_ALIGNMENT_STRATEGY = NodeAlignmentStrategy.fromTypes(KotlinAlignmentStrategy.wrap(null))

private class KotlinFormatter(source: String, psiFactory: KtPsiFactory, val initialIndent: Int, val lineSeparator: String) {
    
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

private fun createPsiFactory(javaProject: IJavaProject): KtPsiFactory {
    val environment = KotlinEnvironment.getEnvironment(javaProject)
    val ideaProject = environment.getProject()
    return KtPsiFactory(ideaProject)
}

private fun createKtFile(source: String, psiFactory: KtPsiFactory): KtFile {
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
    
    override fun getPreviousNonWhitespaceLeaf(node: com.intellij.lang.ASTNode?): com.intellij.lang.ASTNode? {
        if (node == null) return null
        val treePrev = node.treePrev
        if (treePrev != null) {
            val candidate = TreeUtil.getLastChild(treePrev)
            if (candidate != null && !isWhitespaceOrEmpty(candidate)) {
                return candidate
            } else {
                return getPreviousNonWhitespaceLeaf(candidate)
            }
        }
        val treeParent = node.treeParent
    
        if (treeParent == null || treeParent.treeParent == null) {
            return null
        } else {
            return getPreviousNonWhitespaceLeaf(treeParent)
        }
    }
    
    override fun isWhitespaceOrEmpty(node: com.intellij.lang.ASTNode?): kotlin.Boolean {
        if (node == null) return false
        val type = node.elementType
        return type === TokenType.WHITE_SPACE || type !== TokenType.ERROR_ELEMENT && node.textLength == 0
    }
}