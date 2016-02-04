package com.intellij.formatting;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.formatting.Indent.Type;
import com.intellij.openapi.util.TextRange;

public class FormatterFactory implements IndentFactory, SpacingFactory, WrapFactory {
    private final IndentImpl NONE_INDENT = new IndentImpl(Indent.Type.NONE, false, false);
    private final IndentImpl myAbsoluteNoneIndent = new IndentImpl(Indent.Type.NONE, true, false);
    private final IndentImpl myLabelIndent = new IndentImpl(Indent.Type.LABEL, false, false);
    private final IndentImpl myContinuationIndentRelativeToDirectParent = new IndentImpl(Indent.Type.CONTINUATION, false, true);
    private final IndentImpl myContinuationIndentNotRelativeToDirectParent = new IndentImpl(Indent.Type.CONTINUATION, false, false);
    private final IndentImpl myContinuationWithoutFirstIndentRelativeToDirectParent
      = new IndentImpl(Indent.Type.CONTINUATION_WITHOUT_FIRST, false, true);
    private final IndentImpl myContinuationWithoutFirstIndentNotRelativeToDirectParent
      = new IndentImpl(Indent.Type.CONTINUATION_WITHOUT_FIRST, false, false);
    private final IndentImpl myAbsoluteLabelIndent = new IndentImpl(Indent.Type.LABEL, true, false);
    private final IndentImpl myNormalIndentRelativeToDirectParent = new IndentImpl(Indent.Type.NORMAL, false, true);
    private final IndentImpl myNormalIndentNotRelativeToDirectParent = new IndentImpl(Indent.Type.NORMAL, false, false);
    
    private final SpacingImpl myReadOnlySpacing = new SpacingImpl(0, 0, 0, true, false, true, 0, false, 0);
    
    public FormatterFactory() {
        Indent.setFactory(this);
        Spacing.setFactory(this);
        Wrap.setFactory(this);
    }
    
    
    @Override
    public Indent getAbsoluteLabelIndent() {
        return myAbsoluteLabelIndent;
    }

    @Override
    public Indent getAbsoluteNoneIndent() {
        return myAbsoluteNoneIndent;
    }

    @Override
    public Indent getContinuationIndent(boolean relative) {
        return relative ? myContinuationIndentRelativeToDirectParent : myContinuationIndentNotRelativeToDirectParent;
    }

    @Override
    public Indent getContinuationWithoutFirstIndent(boolean relative) {
        return relative ? myContinuationWithoutFirstIndentRelativeToDirectParent : myContinuationWithoutFirstIndentNotRelativeToDirectParent;
    }

    @Override
    public Indent getIndent(Type type, boolean relativeToDirectParent, boolean enforceIndentToChildren) {
        return getIndent(type, 0, relativeToDirectParent, enforceIndentToChildren);
    }

    @Override
    public Indent getIndent(Type type, int spaces, boolean relativeToDirectParent, boolean enforceIndentToChildren) {
        return new IndentImpl(type, false, spaces, relativeToDirectParent, enforceIndentToChildren);
    }

    @Override
    public Indent getLabelIndent() {
        return myLabelIndent;
    }

    @Override
    public Indent getNoneIndent() {
        return NONE_INDENT;
    }

    @Override
    public Indent getNormalIndent(boolean relative) {
        return relative ? myNormalIndentRelativeToDirectParent : myNormalIndentNotRelativeToDirectParent;
    }

    @Override
    public Indent getSmartIndent(Type arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Indent getSpaceIndent(int spaces, boolean relative) {
        return getIndent(Indent.Type.SPACES, spaces, relative, false);
    }


    @Override
    public Spacing createDependentLFSpacing(int minSpaces,
            int maxSpaces,
            @NotNull TextRange dependencyRange,
            boolean keepLineBreaks,
            int keepBlankLines,
            @NotNull DependentSpacingRule rule) {
        return new DependantSpacingImpl(minSpaces, maxSpaces, dependencyRange, keepLineBreaks, keepBlankLines, rule);
    }


    @Override
    public Spacing createDependentLFSpacing(int minSpaces,
            int maxSpaces,
            @NotNull List<TextRange> dependentRegion,
            boolean keepLineBreaks,
            int keepBlankLines,
            @NotNull DependentSpacingRule rule) {
        // TODO Auto-generated method stub
        return new DependantSpacingImpl(minSpaces, maxSpaces, dependentRegion, keepLineBreaks, keepBlankLines, rule);
    }


    @Override
    public Spacing createKeepingFirstColumnSpacing(final int minSpace,
            final int maxSpace,
            final boolean keepLineBreaks,
            final int keepBlankLines) {
        return new SpacingImpl(minSpace, maxSpace, -1, false, false, keepLineBreaks, keepBlankLines, true, 0);
    }


    @Override
    public Spacing createSafeSpacing(final boolean shouldKeepLineBreaks, final int keepBlankLines) {
        return new SpacingImpl(0, 0, 0, false, true, shouldKeepLineBreaks, keepBlankLines, false, 0);
    }


    @Override
    public Spacing createSpacing(int minOffset,
            int maxOffset,
            int minLineFeeds,
            final boolean keepLineBreaks,
            final int keepBlankLines) {
        return new SpacingImpl(minOffset, maxOffset, minLineFeeds, false, false, keepLineBreaks, keepBlankLines,false, 0);
    }


    @Override
    public Spacing createSpacing(final int minSpaces, 
            final int maxSpaces, 
            final int minLineFeeds, 
            final boolean keepLineBreaks, 
            final int keepBlankLines,
            final int prefLineFeeds) {
        return new SpacingImpl(minSpaces, maxSpaces, minLineFeeds, false, false, keepLineBreaks, keepBlankLines, false, prefLineFeeds);
    }


    @Override
    public Spacing getReadOnlySpacing() {
        return myReadOnlySpacing;
    }


    @Override
    public Wrap createChildWrap(Wrap arg0, WrapType arg1, boolean arg2) {
        return new WrapImpl();
    }


    @Override
    public Wrap createWrap(WrapType arg0, boolean arg1) {
        return new WrapImpl();
    }

}
