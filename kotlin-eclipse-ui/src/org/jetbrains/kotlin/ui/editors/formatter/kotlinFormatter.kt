package org.jetbrains.kotlin.ui.editors.formatter

import org.eclipse.jface.text.IDocument
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.ui.formatter.KotlinBlock
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.ui.formatter.NullAlignmentStrategy
import com.intellij.formatting.Indent
import org.jetbrains.kotlin.idea.common.formatter.createSpacingBuilder
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

var settings = CodeStyleSettings(true)
val kotlinSettings = settings.getCustomSettings(KotlinCodeStyleSettings::class.java)

class KotlinFormatter(val editor: KotlinFileEditor) {
    val file = editor.getFile()!!
    
    val lineSeparator = TextUtilities.getDefaultLineDelimiter(editor.document)
    
    val ktFile = editor.parsedFile!!
    
    fun formatCode() {
        val rootBlock = KotlinBlock(ktFile.getNode(), 
                NullAlignmentStrategy(), 
                Indent.getNoneIndent(), 
                null,
                settings,
                createSpacingBuilder(settings))
        
        val edits = format(rootBlock, 0)
        
        val fileChange = TextFileChange("Introduce variable", file)
        edits.forEach { TextChangeCompatibility.addTextEdit(fileChange, "Kotlin change", it.edit) }
        
        fileChange.perform(NullProgressMonitor())
    }
    
    private fun format(parent: ASTBlock, indent: Int): ArrayList<FileEdit> {
        val edits = ArrayList<FileEdit>()
        val subBlocks = parent.getSubBlocks()
        var left = subBlocks.firstOrNull()
        var first = true
        if (left is ASTBlock) 
        for (block in subBlocks) {
            var myIndent = indent
            if (first) {
                first = false
                
                
                if (block is ASTBlock) {
                    edits.addAll(format(block, indent))
                    val blockSubBlocks = block.getSubBlocks()
                    if (blockSubBlocks.size == 1 && block.node.textRange == (blockSubBlocks[0] as ASTBlock).node.textRange) {
                        val edit = addSpacingBefore(blockSubBlocks[0] as ASTBlock)
                        if (edit != null) edits.add(edit)
                    }
                }
                
                continue
            }
            if (block.indent?.type == Type.NORMAL) {
                myIndent++
            }
            val edit = adjustSpacing(parent, left!!, block, myIndent)
            if (edit != null) edits.add(edit)
            
            if (block is ASTBlock) edits.addAll(format(block, myIndent))
            
            left = block
        }
        
        return edits
    }
    
    private fun addSpacingBefore(block: ASTBlock): FileEdit? {
        if (block.indent?.type != Type.NORMAL) return null
        
        val prevParent = block.node.treeParent.treePrev
        if (prevParent !is PsiWhiteSpace) return null
        
        if (IndenterUtil.getLineSeparatorsOccurences(prevParent.getText()) == 0) return null
        
        val indent = IndenterUtil.createWhiteSpace(1, 0, lineSeparator)
        val offset = LineEndUtil.convertLfToDocumentOffset(ktFile.getText(), block.getTextRange().getStartOffset(), editor.document)
        
        return FileEdit(
                    file, 
                    ReplaceEdit(
                            offset, 
                            0,
                            indent))
    }
    
    private fun adjustSpacing(parent: ASTBlock, left: Block, right: Block, indent: Int): FileEdit? {
        val spacing = parent.getSpacing(left, right)
        
        if (left is ASTBlock && right is ASTBlock) {
            val next = left.getNode().getTreeNext().let { 
                if (it == null) return@let null
                when (it.psi) {
                    is KtImportList -> it.getTreeNext()
                    else -> it
                }
            }
            if (next == null) return null
            
            val whiteSpace = if (next is PsiWhiteSpace) next.getText() else ""
            val fixedSpace = fixSpacing(whiteSpace, spacing, indent, right.indent)
            if (fixedSpace == next.getText()) return null
            
            val leftOffset = LineEndUtil.convertLfToDocumentOffset(ktFile.getText(), left.getTextRange().getEndOffset(), editor.document)
            val rightOffset = LineEndUtil.convertLfToDocumentOffset(ktFile.getText(), right.getTextRange().getStartOffset(), editor.document)
            return FileEdit(
                    file, 
                    ReplaceEdit(
                            leftOffset, 
                            rightOffset - leftOffset,
                            fixedSpace))
        }
        
        return null
    }
    
    private fun fixSpacing(whiteSpace: String, spacing: Spacing?, indent: Int, rightIndent: Indent?): String {
        val fixedSpacing = StringBuilder()
        if (spacing is SpacingImpl) {
            val countLineFeeds = IndenterUtil.getLineSeparatorsOccurences(whiteSpace)
            val lineFeeds = getLineFeeds(spacing)
            if (countLineFeeds < lineFeeds || (lineFeeds < countLineFeeds && !spacing.shouldKeepLineFeeds())) {
                if (lineFeeds == 0) {
                    fixedSpacing.append(" ".repeat(spacing.minSpaces))
                } else {
                    fixedSpacing.append(IndenterUtil.createWhiteSpace(indent, lineFeeds, lineSeparator))
                }
            } else if (countLineFeeds != 0) {
//                val biasedIndent = if (rightIndent?.type == Type.NORMAL) indent + 1 else indent
                fixedSpacing.append(IndenterUtil.createWhiteSpace(indent, countLineFeeds, lineSeparator))
            } else if (countLineFeeds == 0) {
                val countSpaces = whiteSpace.length
                if (countSpaces < spacing.minSpaces) {
                    fixedSpacing.append(" ".repeat(spacing.minSpaces))
                } else if (spacing.maxSpaces < countSpaces) {
                    fixedSpacing.append(" ".repeat(spacing.maxSpaces))
                } else {
                    fixedSpacing.append(whiteSpace)
                }
            } else {
                fixedSpacing.append(whiteSpace)
            }
        } else {
            val countLineFeeds = IndenterUtil.getLineSeparatorsOccurences(whiteSpace)
            if (countLineFeeds != 0) {
                fixedSpacing.append(IndenterUtil.createWhiteSpace(indent, countLineFeeds, lineSeparator))
            } else {
                fixedSpacing.append(whiteSpace)
            }
        }
        
        return fixedSpacing.toString()
    }
    
    private fun getLineFeeds(spacing: SpacingImpl): Int {
        return when (spacing) {
            is DependantSpacingImpl -> {
                val trigger = spacing.getDependentRegionRanges().find { 
                    IndenterUtil.getLineSeparatorsOccurences(ktFile.getText().substring(it.startOffset, it.endOffset)) != 0
                }
                
                if (trigger != null) {
                    spacing.setDependentRegionLinefeedStatusChanged()
                }
                
                spacing.minLineFeeds
            }
            else -> spacing.minLineFeeds
        }
    }
}
