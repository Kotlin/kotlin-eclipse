package com.intellij.formatting

open class SpacingImpl(
        val minSpaces: Int,
        maxSpaces: Int,
        private val minLineFeeds: Int,
        isReadOnly: Boolean,
        safe: Boolean,
        shouldKeepLineBreaks: Boolean,
        keepBlankLines: Int,
        keepFirstColumn: Boolean,
        val myPrefLineFeeds: Int) : Spacing() {
    
    companion object {
        private val READ_ONLY_MASK = 1
        private val SAFE_MASK = 2
        private val SHOULD_KEEP_LINE_BREAKS_MASK = 4
        private val SHOULD_KEEP_FIRST_COLUMN_MASK = 8
    }
    
    open val keepBlankLines: Int
    val maxSpaces: Int
    protected var myFlags = 0
    
    init {
        this.maxSpaces = Math.max(minSpaces, maxSpaces)
        if (minLineFeeds > 1 && (minLineFeeds - 1) > keepBlankLines) {
            this.keepBlankLines = minLineFeeds - 1
        } else {
            this.keepBlankLines = keepBlankLines
        }
        
        myFlags = (if (isReadOnly) READ_ONLY_MASK else 0) or 
                (if (safe) SAFE_MASK else 0) or 
                (if (shouldKeepLineBreaks) SHOULD_KEEP_LINE_BREAKS_MASK else 0) or
                (if (keepFirstColumn) SHOULD_KEEP_FIRST_COLUMN_MASK else 0)
    }
    
    open fun getMinLineFeeds(): Int = minLineFeeds
    
    internal fun isReadOnly(): Boolean = (myFlags and READ_ONLY_MASK) != 0
    
    internal fun containsLineFeeds(): Boolean = minLineFeeds > 0
    
    fun isSafe(): Boolean = (myFlags and SAFE_MASK) != 0
    
    fun shouldKeepLineFeeds(): Boolean = (myFlags and SHOULD_KEEP_LINE_BREAKS_MASK) != 0
    
    fun shouldKeepFirstColumn(): Boolean = (myFlags and SHOULD_KEEP_FIRST_COLUMN_MASK) != 0
    
    override fun equals(other: Any?): Boolean {
        if (!(other is SpacingImpl)) return false
        
        return myFlags == other.myFlags &&
            minSpaces == other.minSpaces &&
            maxSpaces == other.maxSpaces &&
            minLineFeeds == other.minLineFeeds &&
            myPrefLineFeeds == other.myPrefLineFeeds &&
            keepBlankLines == other.keepBlankLines
    }
    
    override fun hashCode(): Int {
        return minSpaces + maxSpaces * 29 + minLineFeeds * 11 + myFlags + keepBlankLines + myPrefLineFeeds
    }
    
    override fun toString():String {
        return "<Spacing: minSpaces=" + minSpaces + " maxSpaces=" + maxSpaces + " minLineFeeds=" + minLineFeeds + ">"
    }
    
    fun getPrefLineFeeds(): Int = if (myPrefLineFeeds >= minLineFeeds) myPrefLineFeeds else minLineFeeds
}