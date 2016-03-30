package com.intellij.formatting

import org.jetbrains.annotations.NonNls

open class IndentImpl(
        private val myType: Type?, 
        val isAbsolute: Boolean,
        val spaces: Int, 
        val isRelativeToDirectParent: Boolean,
        open val isEnforceIndentToChildren: Boolean) : Indent() {
    
    constructor(myType: Type?, 
        isAbsolute: Boolean,
        isRelativeToDirectParent: Boolean) : this(myType, isAbsolute, 0, isRelativeToDirectParent, false)
    
    override fun getType(): Indent.Type? = myType
    
    @NonNls
    override fun toString():String {
        if (type == Type.SPACES) {
            return "<Indent: SPACES(" + spaces + ")>"
        }
        
        return "<Indent: " + type + (if (isAbsolute) ":ABSOLUTE " else "") +
                (if (isRelativeToDirectParent) " relative to direct parent " else "") +
                (if (isEnforceIndentToChildren) " enforce indent to children" else "") + ">"
    }
}