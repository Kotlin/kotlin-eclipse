package org.jetbrains.kotlin.ui.tests.scripts.navigation;

import org.jetbrains.kotlin.ui.tests.editors.navigation.KotlinNavigationTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Script support will be fixed in future releases")
public class BasicNavigationInScripts extends KotlinNavigationTestCase {
    @Before
    public void before() {
        configureProjectWithStdLib();
    }
    
    @Override
    protected String getTestDataRelativePath() {
        return "navigation/scripts/basic";
    }
    
    @Test
    public void toFunction() {
        doAutoTest();
    }
    
    @Test
    public void toVariable() {
        doAutoTest();
    }
}
