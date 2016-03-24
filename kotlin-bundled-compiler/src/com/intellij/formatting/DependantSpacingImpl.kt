package com.intellij.formatting

import java.util.ArrayList
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil

open class DependantSpacingImpl(
        minSpaces: Int,
        maxSpaces: Int,
        val dependentRegionRanges: List<TextRange>,
        keepLineBreaks: Boolean,
        keepBlankLines: Int,
        private val myRule: DependentSpacingRule) : 
            SpacingImpl(minSpaces, maxSpaces, 0, false, false, keepLineBreaks, keepBlankLines, false, 0) {
    
    companion object {
        private val DEPENDENCE_CONTAINS_LF_MASK = 0x10
        private val DEPENDENT_REGION_LF_CHANGED_MASK = 0x20
    }
    
    constructor(
        minSpaces:Int,
        maxSpaces:Int,
        dependency:TextRange,
        keepLineBreaks:Boolean,
        keepBlankLines:Int,
        rule:DependentSpacingRule) : this(minSpaces, maxSpaces, listOf(dependency), keepLineBreaks, keepBlankLines, rule)
    
    override fun getMinLineFeeds(): Int {
        if (!isTriggered()) {
            return super.getMinLineFeeds()
        }
        
        if (myRule.hasData(DependentSpacingRule.Anchor.MIN_LINE_FEEDS)) {
            return myRule.getData(DependentSpacingRule.Anchor.MIN_LINE_FEEDS)
        }
        
        if (myRule.hasData(DependentSpacingRule.Anchor.MAX_LINE_FEEDS)) {
            return myRule.getData(DependentSpacingRule.Anchor.MAX_LINE_FEEDS)
        }
        
        return super.getMinLineFeeds()
    }
    
    override val keepBlankLines: Int
        get() {
            if (!isTriggered() || !myRule.hasData(DependentSpacingRule.Anchor.MAX_LINE_FEEDS)) {
                return super.keepBlankLines
            }
            
            return 0
        }
    
    fun isDependentRegionLinefeedStatusChanged(): Boolean = (myFlags and DEPENDENT_REGION_LF_CHANGED_MASK) !== 0
    
    fun setDependentRegionLinefeedStatusChanged() {
        myFlags = myFlags or DEPENDENT_REGION_LF_CHANGED_MASK
        if (getMinLineFeeds() <= 0) {
            myFlags = myFlags or DEPENDENCE_CONTAINS_LF_MASK
        } else {
            myFlags = myFlags and DEPENDENCE_CONTAINS_LF_MASK.inv()
        }
    }
    
    fun getRule(): DependentSpacingRule = myRule
    
    private fun isTriggered(): Boolean {
        return (myRule.getTrigger() == DependentSpacingRule.Trigger.HAS_LINE_FEEDS) xor ((myFlags and DEPENDENCE_CONTAINS_LF_MASK) === 0)
    }
}