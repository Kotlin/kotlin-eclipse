package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport;

import org.junit.Test;

public class KotlinMakeClassOpenQuickFixTest extends KotlinQuickFixTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/quickfix/modifiers/addOpenToClassDeclaration";
    }
    
    @Test
    public void explicitlyFinalSupertype() {
        doAutoTest();
    }

    @Test
    public void explicitlyFinalUpperBound() {
        doAutoTest();
    }

    @Test
    public void finalSupertype() {
        doAutoTest();
    }

    @Test
    public void finalUpperBound() {
        doAutoTest();
    }

    @Test
    public void implementTraitFinalSupertype() {
        doAutoTest();
    }

    @Test
    public void nestedFinalClass() {
        doAutoTest();
    }

    @Test
    public void secondaryCtrDelegationInHeader() {
        doAutoTest();
    }

    @Test
    public void secondaryCtrDelegationInSecondary() {
        doAutoTest();
    }

    @Test
    public void withConstructor() {
        doAutoTest();
    }
}
