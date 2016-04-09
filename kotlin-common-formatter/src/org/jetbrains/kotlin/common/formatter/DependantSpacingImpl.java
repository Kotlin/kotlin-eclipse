package org.jetbrains.kotlin.common.formatter;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.formatting.DependentSpacingRule;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.containers.ContainerUtil;

public class DependantSpacingImpl extends SpacingImpl {
    private static final int DEPENDENCE_CONTAINS_LF_MASK = 0x10;
    private static final int DEPENDENT_REGION_LF_CHANGED_MASK = 0x20;
    
    @NotNull
    private final List<TextRange> myDependentRegionRanges;
    @NotNull
    private final DependentSpacingRule myRule;
    
    public DependantSpacingImpl(final int minSpaces, final int maxSpaces, @NotNull TextRange dependency,
            final boolean keepLineBreaks, final int keepBlankLines, @NotNull DependentSpacingRule rule) {
        super(minSpaces, maxSpaces, 0, false, false, keepLineBreaks, keepBlankLines, false, 0);
        myDependentRegionRanges = ContainerUtil.newSmartList(dependency);
        myRule = rule;
    }
    
    public DependantSpacingImpl(final int minSpaces, final int maxSpaces, @NotNull List<TextRange> dependencyRanges,
            final boolean keepLineBreaks, final int keepBlankLines, @NotNull DependentSpacingRule rule) {
        super(minSpaces, maxSpaces, 0, false, false, keepLineBreaks, keepBlankLines, false, 0);
        myDependentRegionRanges = dependencyRanges;
        myRule = rule;
    }
    
    /**
     * @return <code>1</code> if dependency has line feeds; <code>0</code>
     *         otherwise
     */
    @Override
    public int getMinLineFeeds() {
        if (!isTriggered()) {
            return super.getMinLineFeeds();
        }
        
        if (myRule.hasData(DependentSpacingRule.Anchor.MIN_LINE_FEEDS)) {
            return myRule.getData(DependentSpacingRule.Anchor.MIN_LINE_FEEDS);
        }
        
        if (myRule.hasData(DependentSpacingRule.Anchor.MAX_LINE_FEEDS)) {
            return myRule.getData(DependentSpacingRule.Anchor.MAX_LINE_FEEDS);
        }
        return super.getMinLineFeeds();
    }
    
    @Override
    public int getKeepBlankLines() {
        if (!isTriggered() || !myRule.hasData(DependentSpacingRule.Anchor.MAX_LINE_FEEDS)) {
            return super.getKeepBlankLines();
        }
        
        return 0;
    }
    
    @NotNull
    public List<TextRange> getDependentRegionRanges() {
        return myDependentRegionRanges;
    }
    
    /**
     * Allows to answer whether 'contains line feed' status has been changed for
     * the target dependent region during formatting.
     *
     * @return <code>true</code> if target 'contains line feed' status has been
     *         changed for the target dependent region during formatting;
     *         <code>false</code> otherwise
     */
    public final boolean isDependentRegionLinefeedStatusChanged() {
        return (myFlags & DEPENDENT_REGION_LF_CHANGED_MASK) != 0;
    }
    
    /**
     * Allows to set {@link #isDependentRegionLinefeedStatusChanged() 'dependent
     * region changed'} property.
     */
    public final void setDependentRegionLinefeedStatusChanged() {
        myFlags |= DEPENDENT_REGION_LF_CHANGED_MASK;
        if (getMinLineFeeds() <= 0)
            myFlags |= DEPENDENCE_CONTAINS_LF_MASK;
        else
            myFlags &= ~DEPENDENCE_CONTAINS_LF_MASK;
    }
    
    @Override
    public String toString() {
        return "<DependantSpacing: minSpaces=" + getMinSpaces() + " maxSpaces=" + getMaxSpaces() + " minLineFeeds="
                + getMinLineFeeds() + " dep=NotSupported>";
    }
    
    private boolean isTriggered() {
        return myRule.getTrigger() == DependentSpacingRule.Trigger.HAS_LINE_FEEDS
                ^ (myFlags & DEPENDENCE_CONTAINS_LF_MASK) == 0;
    }
}
