package org.jetbrains.kotlin.common.formatter;

import org.jetbrains.annotations.NotNull;

import com.intellij.formatting.Alignment;

class AlignmentImpl extends Alignment {
    private final boolean myAllowBackwardShift;
    private final Anchor myAnchor;
    
    /**
     * Creates new <code>AlignmentImpl</code> object with <code>'false'</code>
     * as <code>'allows backward shift'</code> argument flag.
     */
    AlignmentImpl() {
        this(false, Anchor.LEFT);
    }
    
    /**
     * Creates new <code>AlignmentImpl</code> object with the given
     * <code>'allows backward shift'</code> argument flag.
     *
     * @param allowBackwardShift
     *            flag that indicates if it should be possible to shift former
     *            aligned block to right in order to align to subsequent aligned
     *            block (see {@link Alignment#createAlignment(boolean, Anchor)})
     * @param anchor
     *            alignment anchor (see
     *            {@link Alignment#createAlignment(boolean, Anchor)})
     */
    AlignmentImpl(boolean allowBackwardShift, @NotNull Anchor anchor) {
        myAllowBackwardShift = allowBackwardShift;
        myAnchor = anchor;
    }
    
    public boolean isAllowBackwardShift() {
        return myAllowBackwardShift;
    }
    
    @NotNull
    public Anchor getAnchor() {
        return myAnchor;
    }
    
    public String getId() {
        return String.valueOf(System.identityHashCode(this));
    }
    
    @Override
    public String toString() {
        return "Align: " + System.identityHashCode(this) + "," + getAnchor() + (isAllowBackwardShift() ? "<" : "");
    }
}
