package org.jetbrains.kotlin.ui.formatter;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;

public abstract class AbstractBlock implements ASTBlock {
    public static final List<Block> EMPTY = Collections.emptyList();
    @NotNull
    protected final ASTNode myNode;
    @Nullable
    protected final Wrap myWrap;
    @Nullable
    protected final Alignment myAlignment;
    
    private List<Block> mySubBlocks;
    private Boolean myIncomplete;
    private boolean myBuildIndentsOnly = false;
    
    protected AbstractBlock(@NotNull ASTNode node, @Nullable Wrap wrap, @Nullable Alignment alignment) {
        myNode = node;
        myWrap = wrap;
        myAlignment = alignment;
    }
    
    @Override
    @NotNull
    public TextRange getTextRange() {
        return myNode.getTextRange();
    }
    
    @Override
    @NotNull
    public List<Block> getSubBlocks() {
        if (mySubBlocks == null) {
            mySubBlocks = buildChildren();
        }
        return mySubBlocks;
    }
    
    /**
     * Prevents from building injected blocks, which allows to build blocks
     * faster Initially was made for formatting-based indent detector
     */
    public void setBuildIndentsOnly(boolean value) {
        myBuildIndentsOnly = value;
    }
    
    protected boolean isBuildIndentsOnly() {
        return myBuildIndentsOnly;
    }
    
    protected abstract List<Block> buildChildren();
    
    @Nullable
    @Override
    public Wrap getWrap() {
        return myWrap;
    }
    
    @Override
    public Indent getIndent() {
        return null;
    }
    
    @Nullable
    @Override
    public Alignment getAlignment() {
        return myAlignment;
    }
    
    @NotNull
    @Override
    public ASTNode getNode() {
        return myNode;
    }
    
    @Override
    @NotNull
    public ChildAttributes getChildAttributes(final int newChildIndex) {
        return new ChildAttributes(getChildIndent(), getFirstChildAlignment());
    }
    
    @Nullable
    private Alignment getFirstChildAlignment() {
        List<Block> subBlocks = getSubBlocks();
        for (final Block subBlock : subBlocks) {
            Alignment alignment = subBlock.getAlignment();
            if (alignment != null) {
                return alignment;
            }
        }
        return null;
    }
    
    @Nullable
    protected Indent getChildIndent() {
        return null;
    }
    
    @Override
    public boolean isIncomplete() {
        if (myIncomplete == null) {
            myIncomplete = isIncomplete(getNode());
        }
        return myIncomplete;
    }
    
    @Override
    public String toString() {
        return myNode.getText() + " " + getTextRange();
    }
    
    public static boolean isIncomplete(@Nullable ASTNode node) {
        ASTNode lastChild = node == null ? null : node.getLastChildNode();
        while (lastChild != null && lastChild.getElementType() == TokenType.WHITE_SPACE) {
            lastChild = lastChild.getTreePrev();
        }
        if (lastChild == null)
            return false;
        if (lastChild.getElementType() == TokenType.ERROR_ELEMENT)
            return true;
        return isIncomplete(lastChild);
    }
}