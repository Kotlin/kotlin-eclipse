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

class KotlinBlock(node: ASTNode,
        private val myAlignmentStrategy: CommonAlignmentStrategy,
        val myIndent: Indent?,
        wrap: Wrap?,
        private val mySettings: CodeStyleSettings,
        private val mySpacingBuilder: KotlinSpacingBuilder) : AbstractBlock(node, wrap, myAlignmentStrategy.getAlignment(node)), KotlinCommonBlock {
            
    private var mySubBlocks: List<Block>? = null
    
    override fun isLeaf(): Boolean = myNode.firstChildNode == null
    
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
}

class NullAlignmentStrategy : CommonAlignmentStrategy() {
    override fun getAlignment(node: ASTNode): Alignment? = null
}