package org.jetbrains.kotlin.ui.formatter

import org.eclipse.jface.text.IDocument
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.formatting.Indent
import org.jetbrains.kotlin.idea.formatter.createSpacingBuilder
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import com.intellij.formatting.Indent.Type
import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Block
import com.intellij.formatting.Spacing
import com.intellij.formatting.DependentSpacingRule
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import com.intellij.formatting.Alignment
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.formatter.KotlinBlock
import org.jetbrains.kotlin.formatter.NULL_ALIGNMENT_STRATEGY

fun computeAlignment(ktFile: KtFile, offset: Int): IndentInEditor {
    settings.getCommonSettings(KotlinLanguage.INSTANCE).ALIGN_MULTILINE_EXTENDS_LIST = true
    settings.getCommonSettings(KotlinLanguage.INSTANCE).ALIGN_MULTILINE_BINARY_OPERATION = true
    val rootBlock = KotlinBlock(ktFile.node, 
                NULL_ALIGNMENT_STRATEGY, 
                Indent.getNoneIndent(), 
                null,
                settings,
                createSpacingBuilder(settings))
    
    val (blockWithParent, indent, index) = computeBlocks(rootBlock, offset)
    
    val alignmentBlock = getAlignment(blockWithParent)
    if (alignmentBlock != null) {
        return getIndentByAlignment(alignmentBlock.parent!!.block, ktFile)
    }
    
    val block = blockWithParent.block
    if (block.isIncomplete) {
        val currentBlock = if (block.textRange.startOffset == offset) {
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
            null -> {
                indent + 2
            }
            else -> indent
        }
        
        return IndentInEditor.BlockIndent(blockIndent)
    } 
    else {
        val alignmentBlock = getAlignment(blockWithParent)
        if (alignmentBlock != null) {
            return getIndentByAlignment(alignmentBlock.parent!!.block, ktFile)
        }
        
        val currentBlock = if (block.textRange.startOffset == offset && block.isLeaf) {
            blockWithParent.parent?.block ?: block
        } else {
            block
        }
        
        val attributes = currentBlock.getChildAttributes(1)
        val blockIndent = when (attributes.childIndent?.type) {
            Type.NORMAL -> indent + 1
            Type.CONTINUATION -> indent + 2
            Type.CONTINUATION_WITHOUT_FIRST -> {
                if (index != 0) indent + 2 else indent
            }
            else -> indent
        }
        
        return IndentInEditor.BlockIndent(blockIndent)
    }
}

private fun getIncompleteBlock(blockWithParent: BlockWithParent): Block? {
    var current: BlockWithParent? = blockWithParent
    while (current != null && !current.block.isIncomplete) {
        current = current.parent
    }
    
    return current?.block
}

private fun getIndentByAlignment(parent: Block, ktFile: KtFile): IndentInEditor.RawIndent {
    val alignmentBlock = parent.subBlocks.find { it.alignment != null }
    val alignmentStartOffset = alignmentBlock!!.textRange.startOffset - 1
    var parentIndent = 0
    val text = ktFile.node.text
    while (alignmentStartOffset - parentIndent >= 0 && text[alignmentStartOffset - parentIndent] != '\n') {
        parentIndent++
    }
    
    return IndentInEditor.RawIndent(parentIndent)
}

private fun computeBlocks(root: KotlinBlock, offset: Int): BlockWithIndentation {
    var indent = 0
    var currentBlock = BlockWithParent(root, null)
    var childIndex: Int
    
    while (true) {
        childIndex = 0
        val subBlocks = currentBlock.block.getSubBlocks()
        var narrowBlock: Block? = null
        
        for (i in subBlocks.indices) {
            val subBlock = subBlocks[i]
            if (subBlock is KotlinBlock && 
                (offset in subBlock.getTextRange())) {
                narrowBlock = subBlock
                        break
            }
            
            if (subBlock is KotlinBlock && 
                (subBlock.textRange.startOffset >= offset)) {
                
                narrowBlock = if (i > 0) subBlocks[i - 1] else null
                break
            }
            
            childIndex++
        }
        
        if (subBlocks.isNotEmpty() && 
            narrowBlock == null && 
            subBlocks.last() is KotlinBlock && 
            (subBlocks.last().textRange.endOffset > offset ||
            subBlocks.last().isIncomplete)) {
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

private fun getAlignment(blockWithParent: BlockWithParent): BlockWithParent? {
    var current: BlockWithParent? = blockWithParent
    while (current?.parent != null) {
        if (current?.block?.alignment != null) {
            return current
        }
        current = current?.parent
    }
    
    return null
}

private class BlockWithParent(val block: Block, val parent: BlockWithParent?)

sealed class IndentInEditor {
    class RawIndent(val rawIndent: Int) : IndentInEditor()
    
    class BlockIndent(val indent: Int) : IndentInEditor() {
        companion object {
            @JvmField val NO_INDENT = BlockIndent(0)
        }
        
        val rawIndent = indent * IndenterUtil.getDefaultIndent()
    }
}

//object KotlinDependantSpacingFactoryImpl : KotlinDependentSpacingFactory {
//    override fun createLineFeedDependentSpacing(
//            minSpaces: Int,
//            maxSpaces: Int,
//            minimumLineFeeds: Int,
//            keepLineBreaks: Boolean,
//            keepBlankLines: Int,
//            dependency: TextRange,
//            rule: DependentSpacingRule): Spacing {
//        return object : DependantSpacingImpl(minSpaces, maxSpaces, dependency, keepLineBreaks, keepBlankLines, rule) {
////            override fun getMinLineFeeds(): Int {
////                val superMin = super.getMinLineFeeds()
////                return if (superMin == 0) minimumLineFeeds else superMin
////            }
//        }
//    }
//}