package com.intellij.formatting

import com.intellij.formatting.Indent.Type
import com.intellij.openapi.util.TextRange

class FormatterFactory : IndentFactory, SpacingFactory, WrapFactory, AlignmentFactory {
    private val NONE_INDENT = IndentImpl(Indent.Type.NONE, false, false)
    private val myAbsoluteNoneIndent = IndentImpl(Indent.Type.NONE, true, false)
    private val myLabelIndent = IndentImpl(Indent.Type.LABEL, false, false)
    private val myContinuationIndentRelativeToDirectParent = IndentImpl(Indent.Type.CONTINUATION, false, true)
    private val myContinuationIndentNotRelativeToDirectParent = IndentImpl(Indent.Type.CONTINUATION, false, false)
    private val myContinuationWithoutFirstIndentRelativeToDirectParent = IndentImpl(Indent.Type.CONTINUATION_WITHOUT_FIRST, false, true)
    private val myContinuationWithoutFirstIndentNotRelativeToDirectParent = IndentImpl(Indent.Type.CONTINUATION_WITHOUT_FIRST, false, false)
    private val myAbsoluteLabelIndent = IndentImpl(Indent.Type.LABEL, true, false)
    private val myNormalIndentRelativeToDirectParent = IndentImpl(Indent.Type.NORMAL, false, true)
    private val myNormalIndentNotRelativeToDirectParent = IndentImpl(Indent.Type.NORMAL, false, false)
    private val myReadOnlySpacing = SpacingImpl(0, 0, 0, true, false, true, 0, false, 0)
    
    init {
        Indent.setFactory(this)
        Spacing.setFactory(this)
        Wrap.setFactory(this)
        Alignment.setFactory(this)
    }
    
    override fun createAlignment(applyToNonFirstBlocksOnLine: Boolean, anchor: Alignment.Anchor): Alignment {
        return AlignmentImpl(applyToNonFirstBlocksOnLine, anchor)
    }
    
    override fun createChildAlignment(base: Alignment): Alignment {
        return AlignmentImpl() // TODO: add properly child alignment
    }
    
    override fun getAbsoluteLabelIndent(): Indent = myAbsoluteLabelIndent
    
    override fun getAbsoluteNoneIndent(): Indent = myAbsoluteNoneIndent
    
    override fun getContinuationIndent(relative: Boolean): Indent {
        return if (relative) myContinuationIndentRelativeToDirectParent else myContinuationIndentNotRelativeToDirectParent
    }
    
    override fun getContinuationWithoutFirstIndent(relative: Boolean): Indent {
        return if (relative) myContinuationWithoutFirstIndentRelativeToDirectParent else myContinuationWithoutFirstIndentNotRelativeToDirectParent
    }
    
    override fun getIndent(type: Type, relativeToDirectParent: Boolean, enforceIndentToChildren: Boolean): Indent {
        return getIndent(type, 0, relativeToDirectParent, enforceIndentToChildren)
    }
    
    override fun getIndent(type: Type, spaces: Int, relativeToDirectParent: Boolean, enforceIndentToChildren: Boolean):Indent {
        return IndentImpl(type, false, spaces, relativeToDirectParent, enforceIndentToChildren)
    }
    
    override fun getLabelIndent(): Indent = myLabelIndent
    
    override fun getNoneIndent(): Indent = NONE_INDENT
    
    override fun getNormalIndent(relative: Boolean): Indent {
        return if (relative) myNormalIndentRelativeToDirectParent else myNormalIndentNotRelativeToDirectParent
    }
    
    override fun getSmartIndent(arg0: Type): Indent? = null
    
    override fun getSpaceIndent(spaces: Int, relative: Boolean): Indent = getIndent(Indent.Type.SPACES, spaces, relative, false)
    
    override fun createDependentLFSpacing(
                    minSpaces:Int,
                    maxSpaces:Int,
                    dependencyRange:TextRange,
                    keepLineBreaks:Boolean,
                    keepBlankLines:Int,
                    rule:DependentSpacingRule):Spacing {
        return DependantSpacingImpl(minSpaces, maxSpaces, dependencyRange, keepLineBreaks, keepBlankLines, rule)
    }
    
    override fun createDependentLFSpacing(minSpaces:Int,
                    maxSpaces: Int,
                    dependentRegion: List<TextRange>,
                    keepLineBreaks: Boolean,
                    keepBlankLines: Int,
                    rule:DependentSpacingRule): Spacing {
        return DependantSpacingImpl(minSpaces, maxSpaces, dependentRegion, keepLineBreaks, keepBlankLines, rule)
    }
    
    override fun createKeepingFirstColumnSpacing(
                    minSpace: Int,
                    maxSpace: Int,
                    keepLineBreaks: Boolean,
                    keepBlankLines: Int): Spacing {
        return SpacingImpl(minSpace, maxSpace, -1, false, false, keepLineBreaks, keepBlankLines, true, 0)
    }
    
    override fun createSafeSpacing(shouldKeepLineBreaks: Boolean, keepBlankLines: Int): Spacing {
        return SpacingImpl(0, 0, 0, false, true, shouldKeepLineBreaks, keepBlankLines, false, 0)
    }
    
    override fun createSpacing(
                    minOffset: Int,
                    maxOffset: Int,
                    minLineFeeds: Int,
                    keepLineBreaks: Boolean,
                    keepBlankLines: Int):Spacing {
        return SpacingImpl(minOffset, maxOffset, minLineFeeds, false, false, keepLineBreaks, keepBlankLines, false, 0)
    }
    
    override fun createSpacing(
                    minSpaces: Int,
                    maxSpaces: Int,
                    minLineFeeds: Int,
                    keepLineBreaks: Boolean,
                    keepBlankLines: Int,
                    prefLineFeeds: Int): Spacing {
        return SpacingImpl(minSpaces, maxSpaces, minLineFeeds, false, false, keepLineBreaks, keepBlankLines, false, prefLineFeeds)
    }
    
    override fun getReadOnlySpacing(): Spacing = myReadOnlySpacing
    
    override fun createChildWrap(arg0: Wrap, arg1: WrapType, arg2: Boolean): Wrap = WrapImpl()
    
    override fun createWrap(arg0: WrapType, arg1: Boolean): Wrap = WrapImpl()
}