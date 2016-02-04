package com.intellij.formatting;

import org.jetbrains.annotations.NonNls;

class IndentImpl extends Indent {
    private final boolean myIsAbsolute;
    private final boolean myRelativeToDirectParent;
    
    private final Type myType;
    private final int mySpaces;
    private final boolean myEnforceIndentToChildren;
    
    public IndentImpl(final Type type, boolean absolute, boolean relativeToDirectParent) {
        this(type, absolute, 0, relativeToDirectParent, false);
    }
    
    public IndentImpl(final Type type, boolean absolute, final int spaces, boolean relativeToDirectParent,
            boolean enforceIndentToChildren) {
        myType = type;
        myIsAbsolute = absolute;
        mySpaces = spaces;
        myRelativeToDirectParent = relativeToDirectParent;
        myEnforceIndentToChildren = enforceIndentToChildren;
    }
    
    @Override
    public Type getType() {
        return myType;
    }
    
    public int getSpaces() {
        return mySpaces;
    }
    
    /**
     * @return <code>'isAbsolute'</code> property value as defined during
     *         {@link IndentImpl} object construction
     */
    boolean isAbsolute() {
        return myIsAbsolute;
    }
    
    /**
     * Allows to answer if current indent object is configured to anchor direct
     * parent that lays on a different line.
     * <p/>
     * Feel free to check {@link Indent} class-level javadoc in order to get
     * more information and examples about expected usage of this property.
     *
     * @return flag that indicates if this indent should anchor direct parent
     *         that lays on a different line
     */
    public boolean isRelativeToDirectParent() {
        return myRelativeToDirectParent;
    }
    
    /**
     * Allows to answer if current indent object is configured to enforce indent
     * for sub-blocks of composite block that doesn't start new line.
     * <p/>
     * Feel free to check {@link Indent} javadoc for the more detailed
     * explanation of this property usage.
     * 
     * @return <code>true</code> if current indent object is configured to
     *         enforce indent for sub-blocks of composite block that doesn't
     *         start new line; <code>false</code> otherwise
     */
    public boolean isEnforceIndentToChildren() {
        return myEnforceIndentToChildren;
    }
    
    @NonNls
    @Override
    public String toString() {
        if (myType == Type.SPACES) {
            return "<Indent: SPACES(" + mySpaces + ")>";
        }
        return "<Indent: " + myType + (myIsAbsolute ? ":ABSOLUTE " : "")
                + (myRelativeToDirectParent ? " relative to direct parent " : "")
                + (myEnforceIndentToChildren ? " enforce indent to children" : "") + ">";
    }
}