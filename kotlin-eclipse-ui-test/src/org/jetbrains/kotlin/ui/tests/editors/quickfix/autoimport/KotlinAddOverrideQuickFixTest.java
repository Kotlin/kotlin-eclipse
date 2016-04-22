package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport;

import org.junit.Test;

public class KotlinAddOverrideQuickFixTest extends KotlinQuickFixTestCase {

    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/quickfix/modifiers/";
    }
    
    @Test
    public void virtualMethodHidden() {
        doAutoTest();
    }
}
