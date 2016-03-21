package org.jetbrains.kotlin.ui.formatter

import com.intellij.psi.tree.IElementType
import com.intellij.formatting.Alignment
import java.util.HashMap

abstract class KotlinAlignmentStrategy {
    companion object {
        val nullStrategy = wrap(null)
        
        fun wrap(alignment: Alignment?, vararg filterTypes: IElementType): KotlinAlignmentStrategy {
            return SharedAlignmentStrategy(alignment, true, *filterTypes)
        }
        
        fun wrap(alignment: Alignment, ignoreFilterTypes: Boolean, vararg filterTypes: IElementType): KotlinAlignmentStrategy {
            return SharedAlignmentStrategy(alignment, ignoreFilterTypes, *filterTypes)
        }
        
        fun createAlignmentPerTypeStrategy(targetTypes:Collection<IElementType>, allowBackwardShift: Boolean): AlignmentPerTypeStrategy {
            return AlignmentPerTypeStrategy(targetTypes, null, allowBackwardShift, Alignment.Anchor.LEFT)
        }
        
        fun createAlignmentPerTypeStrategy(
                targetTypes: Collection<IElementType>, 
                parentType: IElementType?, 
                allowBackwardShift: Boolean,
                anchor:Alignment.Anchor = Alignment.Anchor.LEFT): AlignmentPerTypeStrategy {
            return AlignmentPerTypeStrategy(targetTypes, parentType, allowBackwardShift, anchor)
        }
    }
    
    fun getAlignment(childType: IElementType?): Alignment? = getAlignment(null, childType)
    
    abstract fun getAlignment(parentType: IElementType?, childType: IElementType?): Alignment?
    
    private class SharedAlignmentStrategy(
            val myAlignment: Alignment?,
            val myIgnoreFilterTypes: Boolean, 
            vararg disabledElementTypes: IElementType) : KotlinAlignmentStrategy() {
        
        private val myFilterElementTypes = disabledElementTypes.toSet()
        
        override fun getAlignment(parentType: IElementType?, childType: IElementType?): Alignment? {
            return if (myFilterElementTypes.contains(childType) xor myIgnoreFilterTypes) myAlignment else null
        }
    }
    
    class AlignmentPerTypeStrategy(
            targetElementTypes: Collection<IElementType>,
            private val myParentType: IElementType?,
            private val myAllowBackwardShift: Boolean,
            anchor: Alignment.Anchor) : KotlinAlignmentStrategy() {
        
        private val myAlignments = HashMap<IElementType, Alignment>()
        init {
            for (elementType in targetElementTypes) {
                myAlignments.put(elementType, Alignment.createAlignment(myAllowBackwardShift, anchor))
            }
        }
        
        override fun getAlignment(parentType: IElementType?, childType: IElementType?): Alignment? {
            if (myParentType != null && parentType != null && myParentType !== parentType) {
                return null
            }
            
            return myAlignments[childType]
        }
        
        fun renewAlignment(elementType: IElementType) {
            myAlignments.put(elementType, Alignment.createAlignment(myAllowBackwardShift))
        }
    }
}