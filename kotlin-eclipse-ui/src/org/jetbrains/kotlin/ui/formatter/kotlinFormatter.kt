package org.jetbrains.kotlin.ui.formatter

import org.eclipse.jface.text.IDocument
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.ui.formatter.KotlinBlock
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.formatting.Indent
import org.jetbrains.kotlin.idea.formatter.createSpacingBuilder
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import com.intellij.formatting.Indent.Type
import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Block
import com.intellij.lang.ASTNode
import com.intellij.formatting.Spacing
import com.intellij.psi.PsiWhiteSpace
import com.intellij.formatting.SpacingImpl
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import com.intellij.psi.PsiImportList
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.ui.refactorings.rename.FileEdit
import org.eclipse.text.edits.TextEdit
import org.eclipse.text.edits.ReplaceEdit
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.jetbrains.kotlin.eclipse.ui.utils.getOffsetByDocument
import com.intellij.psi.PsiElement
import org.eclipse.jface.text.TextUtilities
import java.util.ArrayList
import org.eclipse.ltk.core.refactoring.TextFileChange
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility
import org.eclipse.core.runtime.NullProgressMonitor
import org.jetbrains.kotlin.psi.KtImportList
import com.intellij.formatting.DependantSpacingImpl
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.idea.formatter.CommonAlignmentStrategy
import com.intellij.formatting.Alignment
import com.intellij.formatting.DependentSpacingRule
import org.eclipse.ltk.core.refactoring.DocumentChange
import org.eclipse.jface.text.Document
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.intellij.openapi.util.text.StringUtil

@Volatile var settings = CodeStyleSettings(true)

fun formatCode(source: String, javaProject: IJavaProject, lineSeparator: String): String {
    val firstRun = KotlinFormatter(source, javaProject, lineSeparator).formatCode()
    return KotlinFormatter(firstRun, javaProject, lineSeparator).formatCode()
}

val NULL_ALIGNMENT_STRATEGY = NodeAlignmentStrategy.fromTypes(KotlinAlignmentStrategy.wrap(null))

private class KotlinFormatter(source: String, javaProject: IJavaProject, val lineSeparator: String) {
    val ktFile = createKtFile(source, javaProject)
    
    val sourceDocument = Document(source)
    
    fun formatCode(): String {
        val rootBlock = KotlinBlock(ktFile.getNode(), 
                NULL_ALIGNMENT_STRATEGY, 
                Indent.getNoneIndent(), 
                null,
                settings,
                createSpacingBuilder(settings, KotlinDependantSpacingFactoryImpl))
        
        val edits = format(rootBlock, 0)
        
        val documentChange = DocumentChange("Format code", sourceDocument)
        edits.forEach { TextChangeCompatibility.addTextEdit(documentChange, "Kotlin change", it) }
        
        documentChange.perform(NullProgressMonitor())
        return sourceDocument.get()
    }
    
    private fun format(parent: ASTBlock, indent: Int): ArrayList<ReplaceEdit> {
        val edits = ArrayList<ReplaceEdit>()
        
        if (parent.isLeaf) {
            val edit = addSpacingBefore(parent, indent)
            if (edit != null) edits.add(edit)
        }
        
        val subBlocks = parent.subBlocks
        var left = subBlocks.firstOrNull()
        var first = true
        if (left is ASTBlock) 
        for (subBlock in subBlocks) {
            var subBlockIndent = indent
            when (subBlock.indent?.type) {
                Type.NORMAL -> subBlockIndent++
                Type.CONTINUATION -> subBlockIndent += 2
            }
            
            if (first) {
                first = false
                if (subBlock is ASTBlock) {
                    edits.addAll(format(subBlock, subBlockIndent))
                }
                
                continue
            }
            
            if (parent.indent?.type == Type.CONTINUATION_WITHOUT_FIRST &&
                subBlock.indent?.type == Type.CONTINUATION_WITHOUT_FIRST) {
                subBlockIndent += 2
            }
            
            val edit = adjustSpacing(parent, left!!, subBlock)
            if (edit != null) edits.add(edit)
            
            if (subBlock is ASTBlock) {
                edits.addAll(format(subBlock, subBlockIndent))
            }
            
            left = subBlock
        }
        
        return edits
    }
    
    private fun addSpacingBefore(block: ASTBlock, blockIndent: Int): ReplaceEdit? {
        val startOffset = block.node.startOffset
        if (startOffset < 1) return null
        
        val prevParent = ktFile.findElementAt(startOffset - 1)
        if (prevParent !is PsiWhiteSpace) return null
        
        if (IndenterUtil.getLineSeparatorsOccurences(prevParent.getText()) == 0) return null
        
        val indent = IndenterUtil.createWhiteSpace(blockIndent, 0, lineSeparator)
        val offset = LineEndUtil.convertLfToDocumentOffset(ktFile.getText(), block.getTextRange().getStartOffset(), sourceDocument)
        
        return ReplaceEdit(
                        offset, 
                        0,
                        indent)
    }
    
    private fun adjustSpacing(parent: ASTBlock, left: Block, right: Block): ReplaceEdit? {
        val spacing = parent.getSpacing(left, right)
        
        if (left is ASTBlock && right is ASTBlock) {
            val next = ktFile.findElementAt(right.node.startOffset - 1)
            if (next == null) return null
            
            val whiteSpace = if (next is PsiWhiteSpace) next.getText() else ""
            val fixedSpace = fixSpacing(whiteSpace, spacing)
            if (fixedSpace == next.getText()) return null
            
            val leftOffset = LineEndUtil.convertLfToDocumentOffset(ktFile.getText(), left.getTextRange().getEndOffset(), sourceDocument)
            val rightOffset = LineEndUtil.convertLfToDocumentOffset(ktFile.getText(), right.getTextRange().getStartOffset(), sourceDocument)
            return ReplaceEdit(
                            leftOffset, 
                            rightOffset - leftOffset,
                            fixedSpace)
        }
        
        return null
    }
    
    private fun fixSpacing(whiteSpace: String, spacing: Spacing?): String {
        val fixedSpacing = StringBuilder()
        if (spacing is SpacingImpl) {
            val actualLineFeeds = IndenterUtil.getLineSeparatorsOccurences(whiteSpace)
            val expectedLineFeeds = getLineFeeds(spacing)
            if (actualLineFeeds < expectedLineFeeds || (expectedLineFeeds < actualLineFeeds && !spacing.shouldKeepLineFeeds())) {
                if (expectedLineFeeds == 0) {
                    fixedSpacing.append(createWhitespaces(spacing.minSpaces))
                } else {
                    fixedSpacing.append(IndenterUtil.createWhiteSpace(0, expectedLineFeeds, lineSeparator))
                }
            } else if (actualLineFeeds != 0) {
                fixedSpacing.append(IndenterUtil.createWhiteSpace(0, actualLineFeeds, lineSeparator))
            } else if (actualLineFeeds == 0) {
                val countSpaces = whiteSpace.length
                if (countSpaces < spacing.minSpaces) {
                    fixedSpacing.append(createWhitespaces(spacing.minSpaces))
                } else if (spacing.maxSpaces < countSpaces) {
                    fixedSpacing.append(createWhitespaces(spacing.maxSpaces))
                } else {
                    fixedSpacing.append(whiteSpace)
                }
            } else {
                fixedSpacing.append(whiteSpace)
            }
        } else {
            val countLineFeeds = IndenterUtil.getLineSeparatorsOccurences(whiteSpace)
            if (countLineFeeds != 0) {
                fixedSpacing.append(IndenterUtil.createWhiteSpace(0, countLineFeeds, lineSeparator))
            } else {
                fixedSpacing.append(whiteSpace)
            }
        }
        
        return fixedSpacing.toString()
    }
    
    private fun getLineFeeds(spacing: SpacingImpl): Int {
        return when (spacing) {
            is DependantSpacingImpl -> {
                val hasLineFeeds = spacing.dependentRegionRanges.find { 
                    IndenterUtil.getLineSeparatorsOccurences(ktFile.text.substring(it.startOffset, it.endOffset)) != 0
                }
                
                val triggerWithLineFeeds = spacing.getRule().getTrigger() == DependentSpacingRule.Trigger.HAS_LINE_FEEDS
                if ((triggerWithLineFeeds && hasLineFeeds != null) || (!triggerWithLineFeeds && hasLineFeeds == null)) {
                    spacing.setDependentRegionLinefeedStatusChanged()
                }
                
                spacing.getMinLineFeeds()
            }
            else -> spacing.getMinLineFeeds()
        }
    }
}

private fun createKtFile(source: String, javaProject: IJavaProject): KtFile {
    val environment = KotlinEnvironment.getEnvironment(javaProject)
    val ideaProject = environment.getProject()
    return KtPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(source))
}

private fun createWhitespaces(countSpaces: Int) = IndenterUtil.SPACE_STRING.repeat(countSpaces)