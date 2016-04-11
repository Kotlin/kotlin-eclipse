package org.jetbrains.kotlin.formatter

import org.jetbrains.kotlin.idea.formatter.CommonAlignmentStrategy
import com.intellij.formatting.Alignment
import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.common.formatter.AlignmentStrategy

abstract class NodeAlignmentStrategy : CommonAlignmentStrategy() {
    companion object {
        val nullStrategy = fromTypes(AlignmentStrategy.wrap(null))
        
        fun fromTypes(strategy: AlignmentStrategy): NodeAlignmentStrategy = AlignmentStrategyWrapper(strategy)
    }
    
    private class AlignmentStrategyWrapper(val internalStrategy: AlignmentStrategy) : NodeAlignmentStrategy() {
        override fun getAlignment(node: ASTNode): Alignment? {
            val parent = node.getTreeParent()
            if (parent != null) {
                return internalStrategy.getAlignment(parent.getElementType(), node.getElementType())
            }
            
            return internalStrategy.getAlignment(node.getElementType())
        }
    }
}