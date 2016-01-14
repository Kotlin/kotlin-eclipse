package org.jetbrains.kotlin.ui.tests.editors.highlighting;

import org.junit.Test;

public class KotlinHighlightingPositionUpdaterTest extends KotlinHighlightingPositionUpdaterTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "highlighting/positionUpdater";
    }
    
    @Test
    public void afterFunctionName() {
        doAutoTest();
    }
}
