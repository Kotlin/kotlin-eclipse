package com.intellij.formatting;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.common.formatter.AlignmentImpl;
import org.jetbrains.kotlin.common.formatter.DependantSpacingImpl;
import org.jetbrains.kotlin.common.formatter.IndentImpl;
import org.jetbrains.kotlin.common.formatter.SpacingImpl;
import org.jetbrains.kotlin.common.formatter.WrapImpl;

import com.intellij.openapi.util.TextRange;

public class FormatterFactory
        implements IndentFactory, WrapFactory, AlignmentFactory, SpacingFactory {
    
    private final IndentImpl NONE_INDENT = new IndentImpl(Indent.Type.NONE, false, false);
    private final IndentImpl myAbsoluteNoneIndent = new IndentImpl(Indent.Type.NONE, true, false);
    private final IndentImpl myLabelIndent = new IndentImpl(Indent.Type.LABEL, false, false);
    private final IndentImpl myContinuationIndentRelativeToDirectParent = new IndentImpl(Indent.Type.CONTINUATION,
            false, true);
    private final IndentImpl myContinuationIndentNotRelativeToDirectParent = new IndentImpl(Indent.Type.CONTINUATION,
            false, false);
    private final IndentImpl myContinuationWithoutFirstIndentRelativeToDirectParent = new IndentImpl(
            Indent.Type.CONTINUATION_WITHOUT_FIRST, false, true);
    private final IndentImpl myContinuationWithoutFirstIndentNotRelativeToDirectParent = new IndentImpl(
            Indent.Type.CONTINUATION_WITHOUT_FIRST, false, false);
    private final IndentImpl myAbsoluteLabelIndent = new IndentImpl(Indent.Type.LABEL, true, false);
    private final IndentImpl myNormalIndentRelativeToDirectParent = new IndentImpl(Indent.Type.NORMAL, false, true);
    private final IndentImpl myNormalIndentNotRelativeToDirectParent = new IndentImpl(Indent.Type.NORMAL, false, false);
    private final SpacingImpl myReadOnlySpacing = new SpacingImpl(0, 0, 0, true, false, true, 0, false, 0);
    
    public FormatterFactory() {
        Indent.setFactory(this);
        Wrap.setFactory(this);
        Alignment.setFactory(this);
        Spacing.setFactory(this);
    }
    
    @Override
    public Alignment createAlignment(boolean applyToNonFirstBlocksOnLine, @NotNull Alignment.Anchor anchor) {
        return new AlignmentImpl(applyToNonFirstBlocksOnLine, anchor);
    }
    
    @Override
    public Alignment createChildAlignment(final Alignment base) {
        AlignmentImpl result = new AlignmentImpl();
        return result;
    }
    
    @Override
    public Indent getNormalIndent(boolean relative) {
        return relative ? myNormalIndentRelativeToDirectParent : myNormalIndentNotRelativeToDirectParent;
    }
    
    @Override
    public Indent getNoneIndent() {
        return NONE_INDENT;
    }
    
    
    @Override
    public Wrap createWrap(WrapType type, boolean wrapFirstElement) {
        return new WrapImpl();
    }
    
    @Override
    public Wrap createChildWrap(final Wrap parentWrap, final WrapType wrapType, final boolean wrapFirstElement) {
        final WrapImpl result = new WrapImpl();
        return result;
    }
    
    @Override
    @NotNull
    public Spacing createSpacing(int minOffset, int maxOffset, int minLineFeeds, final boolean keepLineBreaks,
            final int keepBlankLines) {
        return new SpacingImpl(minOffset, maxOffset, minLineFeeds, false, false, keepLineBreaks, keepBlankLines, false, 0);
    }
    
    @Override
    @NotNull
    public Spacing getReadOnlySpacing() {
        return myReadOnlySpacing;
    }
    
    @NotNull
    @Override
    public Spacing createDependentLFSpacing(int minSpaces, int maxSpaces, @NotNull TextRange dependencyRange,
            boolean keepLineBreaks, int keepBlankLines, @NotNull DependentSpacingRule rule) {
        return new DependantSpacingImpl(minSpaces, maxSpaces, dependencyRange, keepLineBreaks, keepBlankLines, rule);
    }
    
    @NotNull
    @Override
    public Spacing createDependentLFSpacing(int minSpaces, int maxSpaces, @NotNull List<TextRange> dependentRegion,
            boolean keepLineBreaks, int keepBlankLines, @NotNull DependentSpacingRule rule) {
        return new DependantSpacingImpl(minSpaces, maxSpaces, dependentRegion, keepLineBreaks, keepBlankLines, rule);
    }
    
    
    @Override
    public Indent getSpaceIndent(final int spaces, final boolean relative) {
        return getIndent(Indent.Type.SPACES, spaces, relative, false);
    }
    
    @Override
    public Indent getIndent(@NotNull Indent.Type type, boolean relativeToDirectParent,
            boolean enforceIndentToChildren) {
        return getIndent(type, 0, relativeToDirectParent, enforceIndentToChildren);
    }
    
    @Override
    public Indent getSmartIndent(@NotNull Indent.Type type) {
        return null;
    }
    
    @Override
    public Indent getIndent(@NotNull Indent.Type type, int spaces, boolean relativeToDirectParent,
            boolean enforceIndentToChildren) {
        return new IndentImpl(type, false, spaces, relativeToDirectParent, enforceIndentToChildren);
    }
    
    @Override
    public Indent getAbsoluteLabelIndent() {
        return myAbsoluteLabelIndent;
    }
    
    @Override
    @NotNull
    public Spacing createSafeSpacing(final boolean shouldKeepLineBreaks, final int keepBlankLines) {
        return new SpacingImpl(0, 0, 0, false, true, shouldKeepLineBreaks, keepBlankLines, false, 0);
    }
    
    @Override
    @NotNull
    public Spacing createKeepingFirstColumnSpacing(final int minSpace, final int maxSpace, final boolean keepLineBreaks,
            final int keepBlankLines) {
        return new SpacingImpl(minSpace, maxSpace, -1, false, false, keepLineBreaks, keepBlankLines, true, 0);
    }
    
    @Override
    @NotNull
    public Spacing createSpacing(final int minSpaces, final int maxSpaces, final int minLineFeeds,
            final boolean keepLineBreaks, final int keepBlankLines, final int prefLineFeeds) {
        return new SpacingImpl(minSpaces, maxSpaces, minLineFeeds, false, false, keepLineBreaks, keepBlankLines, false, prefLineFeeds);
    }
    
    @Override
    public Indent getAbsoluteNoneIndent() {
        return myAbsoluteNoneIndent;
    }
    
    @Override
    public Indent getLabelIndent() {
        return myLabelIndent;
    }
    
    @Override
    public Indent getContinuationIndent(boolean relative) {
        return relative ? myContinuationIndentRelativeToDirectParent : myContinuationIndentNotRelativeToDirectParent;
    }
    
    // is default
    @Override
    public Indent getContinuationWithoutFirstIndent(boolean relative) {
        return relative ? myContinuationWithoutFirstIndentRelativeToDirectParent
                : myContinuationWithoutFirstIndentNotRelativeToDirectParent;
    }
}
