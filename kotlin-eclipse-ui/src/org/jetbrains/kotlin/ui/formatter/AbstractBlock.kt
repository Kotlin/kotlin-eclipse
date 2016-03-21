/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.formatter

import java.util.Collections
import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType

private val EMPTY: List<Block> = emptyList()

private fun isIncomplete(node: ASTNode?): Boolean {
    var lastChild = if (node == null) null else node.getLastChildNode()
    while (lastChild != null && lastChild.getElementType() == TokenType.WHITE_SPACE) {
        lastChild = lastChild.getTreePrev()
    }
    
    if (lastChild == null) return false
    
    if (lastChild.getElementType() === TokenType.ERROR_ELEMENT) return true
    
    return isIncomplete(lastChild)
}

abstract class AbstractBlock(
        private val myNode: ASTNode,
        private val myWrap: Wrap?,
        private val myAlignment: Alignment?) : ASTBlock {
    private var mySubBlocks: List<Block>? = null
    private var myIncomplete: Boolean? = null
    
    protected abstract fun buildChildren(): List<Block>
    
    protected var isBuildIndentsOnly = false
    
    override fun getAlignment(): Alignment? = myAlignment
    
    override fun getNode(): ASTNode = myNode
    
    override fun getWrap(): Wrap? = myWrap
    
    override fun getTextRange(): TextRange = myNode.getTextRange()
    
    override fun getSubBlocks(): List<Block> {
        if (mySubBlocks == null) {
            mySubBlocks = buildChildren()
        }
        return mySubBlocks!!
    }
    
    override fun getIndent(): Indent? = null
    
    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        return ChildAttributes(getChildIndent(), getFirstChildAlignment())
    }
    
    private fun getFirstChildAlignment(): Alignment? {
        return subBlocks.asSequence().mapNotNull { it.alignment }.firstOrNull()
    }
    
    protected fun getChildIndent(): Indent? = null
    
    override fun isIncomplete(): Boolean {
        if (myIncomplete == null) {
            myIncomplete = isIncomplete(node)
        }
        return myIncomplete!!
    }
    
    override fun toString(): String = "${node.text} $textRange"
}