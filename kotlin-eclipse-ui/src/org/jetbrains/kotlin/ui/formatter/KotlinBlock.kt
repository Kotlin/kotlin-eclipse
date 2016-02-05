package org.jetbrains.kotlin.ui.formatter

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.idea.common.formatter.CommonAlignmentStrategy
import com.intellij.formatting.Indent
import com.intellij.formatting.Wrap
import com.intellij.psi.codeStyle.CodeStyleSettings
import org.jetbrains.kotlin.idea.common.formatter.KotlinSpacingBuilder
import org.jetbrains.kotlin.idea.common.formatter.KotlinCommonBlock
import org.jetbrains.kotlin.idea.common.formatter.createChildIndent
import com.intellij.formatting.Block
import com.intellij.formatting.Spacing
import org.jetbrains.kotlin.idea.common.formatter.WrappingStrategy
import org.jetbrains.kotlin.KtNodeTypes.OPERATION_REFERENCE
import com.intellij.formatting.Alignment
import com.intellij.formatting.ChildAttributes
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.lexer.KtTokens.*

private val CODE_BLOCKS = TokenSet.create(
                BLOCK,
                CLASS_BODY,
                FUNCTION_LITERAL)

private val KDOC_COMMENT_INDENT = 1

class KotlinBlock(node: ASTNode,
        private val myAlignmentStrategy: CommonAlignmentStrategy,
        val myIndent: Indent?,
        wrap: Wrap?,
        private val mySettings: CodeStyleSettings,
        private val mySpacingBuilder: KotlinSpacingBuilder) : AbstractBlock(node, wrap, myAlignmentStrategy.getAlignment(node)), KotlinCommonBlock {
            
    private var mySubBlocks: List<Block>? = null
    
    override fun isLeaf(): Boolean = myNode.firstChildNode == null
    
    override fun getIndent(): Indent? = myIndent
    
    override fun buildChildren(): List<Block>? {
        if (mySubBlocks == null) {
            mySubBlocks = buildCommonChildren()
        }
        return mySubBlocks!!
    }
    
    override fun getSpacing(child1: Block?, child2: Block): Spacing? = mySpacingBuilder.getSpacing(this, child1, child2)
    
    override val blockNode: ASTNode = node
    
    override fun constructSubBlock(child: ASTNode, alignmentStrategy: CommonAlignmentStrategy, wrappingStrategy: WrappingStrategy): Block {
        val wrap = wrappingStrategy.getWrap(child.elementType)

        // Skip one sub-level for operators, so type of block node is an element type of operator
        if (child.elementType === OPERATION_REFERENCE) {
            val operationNode = child.firstChildNode
            if (operationNode != null) {
                return KotlinBlock(operationNode, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder)
            }
        }

        return KotlinBlock(child, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder)
    }
    
    override fun getChildrenAlignmentStrategy(): CommonAlignmentStrategy = NullAlignmentStrategy()
    
    override fun getCodeStyleSettings(): CodeStyleSettings = mySettings
    
    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val type = node.elementType
        if (CODE_BLOCKS.contains(type) ||
            type === WHEN ||
            type === IF ||
            type === FOR ||
            type === WHILE ||
            type === DO_WHILE) {

            return ChildAttributes(Indent.getNormalIndent(), null)
        }
        else if (type === TRY) {
            // In try - try BLOCK catch BLOCK finally BLOCK
            return ChildAttributes(Indent.getNoneIndent(), null)
        }
        else if (type === DOT_QUALIFIED_EXPRESSION || type === SAFE_ACCESS_EXPRESSION) {
            return ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null)
        }
        else if (type === VALUE_PARAMETER_LIST || type === VALUE_ARGUMENT_LIST) {
            // Child index 1 - cursor is after ( - parameter alignment should be recreated
            // Child index 0 - before expression - know nothing about it
            if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < subBlocks.size) {
                val block = subBlocks[newChildIndex]
                return ChildAttributes(block.indent, block.alignment)
            }
            return ChildAttributes(Indent.getContinuationIndent(), null)
        }
        else if (type === DOC_COMMENT) {
            return ChildAttributes(Indent.getSpaceIndent(KDOC_COMMENT_INDENT), null)
        }

        if (type === PARENTHESIZED) {
            return super.getChildAttributes(newChildIndex)
        }

        val blocks = subBlocks
        if (newChildIndex != 0) {
            val isIncomplete = if (newChildIndex < blocks.size) blocks[newChildIndex - 1].isIncomplete else isIncomplete
            if (isIncomplete) {
                return super.getChildAttributes(newChildIndex)
            }
        }

        return ChildAttributes(Indent.getNoneIndent(), null)
    }
}

class NullAlignmentStrategy : CommonAlignmentStrategy() {
    override fun getAlignment(node: ASTNode): Alignment? = null
}