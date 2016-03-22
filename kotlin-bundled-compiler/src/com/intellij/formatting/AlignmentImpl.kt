package com.intellij.formatting

class AlignmentImpl(private val allowBackwardShift: Boolean, private val myAnchor: Anchor) : Alignment() {
    constructor(): this(false, Anchor.LEFT)
    
    constructor(myAnchor: Anchor): this(false, myAnchor)
    
    fun getAnchor() = myAnchor
    
    fun isAllowBackwardShift() = allowBackwardShift
}