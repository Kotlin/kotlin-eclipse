package org.jetbrains.kotlin.ui.tests.editors.navigation;

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
    
    @Test
    public void testClassWithMisplacedSource() {
        doAutoTest();
    }
    
    @Test
    public void testFunctionWithMisplacedSource() {
        doAutoTest();
    }
    
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
