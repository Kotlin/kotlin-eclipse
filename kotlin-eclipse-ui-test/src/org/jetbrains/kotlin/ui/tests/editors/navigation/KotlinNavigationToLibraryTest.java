package org.jetbrains.kotlin.ui.tests.editors.navigation;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinNavigationToLibraryTest extends KotlinNavigationToLibraryTestCase {
    
    @Test
    public void testClass() {
        doAutoTest();
    }
    
    @Test
    public void testFunction() {
        doAutoTest();
    }
    
    @Test
    public void testPackageFunction() {
        doAutoTest();
    }

    @Ignore
    @Test
    public void testClassWithMisplacedSource() {
        doAutoTest();
    }

    @Ignore
    @Test
    public void testFunctionWithMisplacedSource() {
        doAutoTest();
    }
    
    @Ignore
    @Test
    public void testClassWithIdenticalMisplacedSource() {
        doAutoTest();
    }
    
    @Test
    public void testConstructor() {
        doAutoTest();
    }  
    
    @Test
    public void testPackageFacadeFunction() {
        doAutoTest();
    }
}
