package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;

public class KotlinRemoveExplicitTypeTest extends KotlinRemoveExplicitTypeTestCase {
    @Test
    public void testNotOnParameterOfFunctionType() {
        doTest("common_testData/ide/intentions/removeExplicitType/notOnParameterOfFunctionType.kt");
    }
    
    @Test
    public void testOnOverride() {
        doTest("common_testData/ide/intentions/removeExplicitType/onOverride.kt");
    }
    
    @Test
    public void testOnOverrideInTrait() {
        doTest("common_testData/ide/intentions/removeExplicitType/onOverrideInTrait.kt");
    }
    
    @Test
    public void testOnType() {
        doTest("common_testData/ide/intentions/removeExplicitType/onType.kt");
    }
    
    @Test
    public void testRemoveUnresolvedType() {
        doTest("common_testData/ide/intentions/removeExplicitType/removeUnresolvedType.kt");
    }
}
