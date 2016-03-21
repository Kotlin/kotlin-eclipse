package org.jetbrains.kotlin.ui.formatter

import org.jetbrains.kotlin.idea.formatter.CommonAlignmentStrategy
import com.intellij.formatting.Alignment
import com.intellij.lang.ASTNode

abstract class NodeAlignmentStrategy : CommonAlignmentStrategy() {
    companion object {
        val nullStrategy = fromTypes(KotlinAlignmentStrategy.wrap(null))
        
        fun fromTypes(strategy: KotlinAlignmentStrategy): NodeAlignmentStrategy = AlignmentStrategyWrapper(strategy)
    }
    
    private class AlignmentStrategyWrapper(val internalStrategy: KotlinAlignmentStrategy) : NodeAlignmentStrategy() {
        override fun getAlignment(node: ASTNode): Alignment? {
            val parent = node.getTreeParent()
            if (parent != null) {
                return internalStrategy.getAlignment(parent.getElementType(), node.getElementType())
            }
            
            return internalStrategy.getAlignment(node.getElementType())
        }
    }
}