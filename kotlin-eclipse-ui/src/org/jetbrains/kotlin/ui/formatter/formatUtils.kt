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
import com.intellij.formatting.DependantSpacingImpl
import com.intellij.formatting.Spacing
import com.intellij.formatting.DependentSpacingRule
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.formatter.KotlinDependentSpacingFactory
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import com.intellij.formatting.Alignment

fun computeAlignment(ktFile: KtFile, offset: Int): IndentInEditor {
    val rootBlock = KotlinBlock(ktFile.node, 
                NULL_ALIGNMENT_STRATEGY, 
                Indent.getNoneIndent(), 
                null,
                settings,
                createSpacingBuilder(settings, KotlinDependantSpacingFactoryImpl))
    
    val (block, parent, indent, index) = computeBlocks(rootBlock, offset)
    
    val alignment = getAlignment(block, parent)
    if (alignment != null) {
        val alignmentBlock = parent.subBlocks[0]
        val alignmentStartOffset = alignmentBlock.textRange.startOffset
        var parentIndent = 0
        val text = ktFile.node.text
        while (alignmentStartOffset - parentIndent > 0 && text[alignmentStartOffset - parentIndent] != '\n') {
            parentIndent++
        }
        
        return IndentInEditor.RawIndent(parentIndent + 1)
    }
    
    val attributes = block.getChildAttributes(index + 1)
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

fun computeBlocks(root: KotlinBlock, offset: Int): BlockWithIndentation {
    var indent = 0
    var currentBlock: Block = root
    var childIndex: Int
    var parent: Block = root
    
    while (true) {
        childIndex = 0
        val subBlocks = currentBlock.getSubBlocks()
        var narrowBlock: Block? = null
        for (subBlock in subBlocks) {
//            if (subBlock is KotlinBlock && 
//                !subBlock.isLeaf &&
//                (offset in subBlock.getTextRange() || offset == subBlock.textRange.endOffset)) {
//                narrowBlock = subBlock
//                break
//            }
            if (subBlock is KotlinBlock && 
                    (offset in subBlock.getTextRange())) {
                narrowBlock = subBlock
                        break
            }
            childIndex++
        }
        
        if (narrowBlock != null) {
            when (narrowBlock.indent?.type) {
                Type.NORMAL -> indent++
                Type.CONTINUATION -> indent += 2
                Type.CONTINUATION_WITHOUT_FIRST -> {
                    if (childIndex != 0 && root.indent?.type == Type.CONTINUATION_WITHOUT_FIRST) indent += 2
                }
            }
            
            parent = currentBlock
            currentBlock = narrowBlock
        } else {
            break
        }
    }
    
    return BlockWithIndentation(currentBlock, parent, indent, childIndex)
}

data class BlockWithIndentation(val block: Block, val parent: Block, val indent: Int, val index: Int)

private fun getAlignment(block: Block, parent: Block): Alignment? = block.alignment ?: parent.alignment

sealed class IndentInEditor {
    class RawIndent(val rawIndent: Int) : IndentInEditor()
    
    class BlockIndent(val indent: Int) : IndentInEditor() {
        companion object {
            @JvmField val NO_INDENT = BlockIndent(0)
        }
        
        val rawIndent = indent * IndenterUtil.getDefaultIndent()
    }
}

object KotlinDependantSpacingFactoryImpl : KotlinDependentSpacingFactory {
    override fun createLineFeedDependentSpacing(
            minSpaces: Int,
            maxSpaces: Int,
            minimumLineFeeds: Int,
            keepLineBreaks: Boolean,
            keepBlankLines: Int,
            dependency: TextRange,
            rule: DependentSpacingRule): Spacing {
        return object : DependantSpacingImpl(minSpaces, maxSpaces, dependency, keepLineBreaks, keepBlankLines, rule) {
//            override fun getMinLineFeeds(): Int {
//                val superMin = super.getMinLineFeeds()
//                return if (superMin == 0) minimumLineFeeds else superMin
//            }
        }
    }
}