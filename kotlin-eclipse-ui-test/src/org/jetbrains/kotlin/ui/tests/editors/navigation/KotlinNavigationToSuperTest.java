package org.jetbrains.kotlin.ui.tests.editors.navigation;

import org.junit.Test;

public class KotlinNavigationToSuperTest extends KotlinNavigationToSuperTestCase {
    @Test
    public void testClassSimple() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/ClassSimple.test");
    }
    
    @Test
    public void testDelegatedFun() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/DelegatedFun.test");
    }
    
    @Test
    public void testDelegatedProperty() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/DelegatedProperty.test");
    }
    
    @Test
    public void testFakeOverrideFun() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/FakeOverrideFun.test");
    }
    
    @Test
    public void testFakeOverrideFunWithMostRelevantImplementation() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/FakeOverrideFunWithMostRelevantImplementation.test");
    }
    
    @Test
    public void testFakeOverrideProperty() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/FakeOverrideProperty.test");
    }
    
    @Test
    public void testFunctionSimple() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/FunctionSimple.test");
    }
    
    @Test
    public void testObjectSimple() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/ObjectSimple.test");
    }
    
    @Test
    public void testPropertySimple() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/PropertySimple.test");
    }
    
    @Test
    public void testSuperWithNativeToGenericMapping() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/SuperWithNativeToGenericMapping.test");
    }
    
    @Test
    public void testTraitSimple() throws Exception {
        doTest("common_testData/ide/navigation/gotoSuper/TraitSimple.test");
    }
}
