package com.intellij.formatting

import com.intellij.formatting.Indent.Type
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.TreeUtil
import org.jetbrains.kotlin.idea.formatter.KotlinSpacingBuilderUtil
import org.jetbrains.kotlin.psi.KtFile

fun computeAlignment(ktFile: KtFile, offset: Int, rootBlock: ASTBlock): IndentInEditor {
    val (blockWithParent, indent, index) = computeBlocks(rootBlock, offset)
    
    val alignmentBlock = getAlignment(blockWithParent)
    if (alignmentBlock != null) {
        return getIndentByAlignment(alignmentBlock.parent!!.block, ktFile)
    }
    
    val block = blockWithParent.block
    
    val currentBlock = if ((block.textRange.startOffset == offset) && (block.isIncomplete || block.isLeaf)) {
            blockWithParent.parent?.block ?: block
        } else {
            block
        }
    
    val attributes = currentBlock.getChildAttributes(index + 1)
    if (attributes.alignment != null) {
        return getIndentByAlignment(currentBlock, ktFile)
    }
    
    val blockIndent = when (attributes.childIndent?.type) {
        Type.NORMAL -> indent + 1
        Type.CONTINUATION -> indent + 2
        Type.CONTINUATION_WITHOUT_FIRST -> {
            if (index != 0) indent + 2 else indent
        }
        null -> indent + 2
        else -> indent
    }
    
    return IndentInEditor.BlockIndent(blockIndent)
}

sealed class IndentInEditor {
    class RawIndent(val rawIndent: Int) : IndentInEditor()
    
    class BlockIndent(val indent: Int) : IndentInEditor() {
        companion object {
            @JvmField val NO_INDENT = BlockIndent(0)
        }
    }
}

fun getIndentByAlignment(parent: Block, ktFile: KtFile): IndentInEditor.RawIndent {
    val alignmentBlock = parent.subBlocks.find { it.alignment != null }
    val alignmentStartOffset = alignmentBlock!!.textRange.startOffset - 1
    var parentIndent = 0
    val text = ktFile.node.text
    while (alignmentStartOffset - parentIndent >= 0 && text[alignmentStartOffset - parentIndent] != '\n') {
        parentIndent++
    }
    
    return IndentInEditor.RawIndent(parentIndent)
}

private fun computeBlocks(root: ASTBlock, offset: Int): BlockWithIndentation {
    var indent = 0
    var currentBlock = BlockWithParent(root, null)
    var childIndex: Int
    
    while (true) {
        childIndex = 0
        val subBlocks = currentBlock.block.getSubBlocks()
        var narrowBlock: Block? = null
        
        for (i in subBlocks.indices) {
            val subBlock = subBlocks[i]
            if (offset in subBlock.getTextRange()) {
                narrowBlock = subBlock
                break
            }
            
            if (offset == subBlock.textRange.endOffset && subBlock.isIncomplete) {
                narrowBlock = subBlock
                break
            }
            
            if (subBlock.textRange.startOffset >= offset) {
                if (i > 0) {
                    val prev = subBlocks[i - 1]
                    narrowBlock = if (prev.isIncomplete) prev else null
                }
                break
            }
            
            childIndex++
        }
        
        if (subBlocks.isNotEmpty() && 
            narrowBlock == null && 
            subBlocks.last().textRange.endOffset < offset &&
            subBlocks.last().isIncomplete) {
            narrowBlock = subBlocks.last()
        }
        
        if (narrowBlock != null) {
            when (narrowBlock.indent?.type) {
                Type.NORMAL -> indent++
                Type.CONTINUATION -> indent += 2
                Type.CONTINUATION_WITHOUT_FIRST -> {
                    if (childIndex != 0 && root.indent?.type == Type.CONTINUATION_WITHOUT_FIRST) indent += 2
                }
            }
            
            currentBlock = BlockWithParent(narrowBlock, currentBlock)
        } else {
            break
        }
    }
    
    return BlockWithIndentation(currentBlock, indent, childIndex)
}

private data class BlockWithIndentation(val block: BlockWithParent, val indent: Int, val index: Int)

fun getAlignment(blockWithParent: BlockWithParent): BlockWithParent? {
    var current: BlockWithParent? = blockWithParent
    val initialOffset = blockWithParent.block.textRange.startOffset
    while (true) {
        val alignment: Alignment? = current?.block?.alignment
        if (alignment != null) {
            val firstWithAlignment = current!!.parent?.block?.subBlocks?.find { it.alignment != null }
            return if (firstWithAlignment != current.block) current else null
        }
        
        current = current?.parent
        if (current == null || current.block.textRange.startOffset != initialOffset) {
            return null
        }
    }
}

class BlockWithParent(val block: Block, val parent: BlockWithParent?)

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