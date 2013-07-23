package org.jetbrains.kotlin.ui.editors;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class AnalyzerSchedulingRule implements ISchedulingRule {
    
    public static final AnalyzerSchedulingRule INSTANCE = new AnalyzerSchedulingRule();
    
    private AnalyzerSchedulingRule() {
    }

    @Override
    public boolean contains(ISchedulingRule rule) {
        return rule == this;
    }

    @Override
    public boolean isConflicting(ISchedulingRule rule) {
        return rule == this;
    }
}