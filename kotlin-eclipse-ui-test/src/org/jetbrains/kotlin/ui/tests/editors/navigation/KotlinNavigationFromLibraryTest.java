package org.jetbrains.kotlin.ui.tests.editors.navigation;

import org.jetbrains.kotlin.ui.tests.editors.navigation.KotlinNavigationFromLibraryTestCase;
import org.junit.Test;

public class KotlinNavigationFromLibraryTest extends KotlinNavigationFromLibraryTestCase {
    
    private final static String TEST_CLASS_NAME = "navtest.LibraryNavigationKt";
   
    @Test
    public void navigateToTheSameFile() {
        doAutoTest(TEST_CLASS_NAME);
    }
    
    @Test
    public void navigateToAnotherFile() {
        doAutoTest(TEST_CLASS_NAME);
    }
}
