package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport;

import org.junit.Test;

public class KotlinMakeOverridenMemberOpenQuickFixTest extends KotlinQuickFixTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/quickfix/override";
    }
    
    // TODO: Fix for several base methods
//    @Test
//    public void overriddingMultipleFinalMethods() {
//        doAutoTest();
//    }
//    
    
    @Test
    public void overridingDelegatedMethod() {
        doAutoTest();
    }
    
    @Test
    public void overridingFakeOverride() {
        doAutoTest();
    }
    
    @Test
    public void overridingFinalMethod() {
        doAutoTest();
    }
    
    @Test
    public void overridingFinalMethodInLocal() {
        doAutoTest();
    }
    
    @Test
    public void overridingFinalProperty() {
        doAutoTest();
    }
}
