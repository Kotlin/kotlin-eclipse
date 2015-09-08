package org.jetbrains.kotlin.ui.tests.debugger;

import org.junit.Test;

public class KotlinStepIntoSelectionTest extends KotlinStepIntoSelectionTestCase {
    @Test
    public void testSimple() {
        doTest("testData/debugger/simple.kt");
    }
    
    @Test
    public void testCallChain1() {
        doTest("testData/debugger/callChain1.kt");
    }
    
    @Test
    public void testCallChain2() {
        doTest("testData/debugger/callChain2.kt");
    }
    
    @Test
    public void testDelegatedPropertyGetter() {
        doTest("testData/debugger/delegatedPropertyGetter.kt");
    }
    
    @Test
    public void testDotQualified1() {
        doTest("testData/debugger/dotQualified1.kt");
    }
    
    @Test
    public void testDotQualified2() {
        doTest("testData/debugger/dotQualified2.kt");
    }
    
    @Test
    public void testDotQualifiedInParam1() {
        doTest("testData/debugger/dotQualifiedInParam1.kt");
    }
    
    @Test
    public void testDotQualifiedInParam2() {
        doTest("testData/debugger/dotQualifiedInParam2.kt");
    }
    
    @Test
    public void testDoWhile() {
        doTest("testData/debugger/doWhile.kt");
    }
    
    @Test
    public void testFor() {
        doTest("testData/debugger/for.kt");
    }
    
    @Test
    public void testFunWithExpressionBody() {
        doTest("testData/debugger/funWithExpressionBody.kt");
    }
    
    @Test
    public void testIf() {
        doTest("testData/debugger/if.kt");
    }
    
    @Test
    public void testInfixCall() {
        doTest("testData/debugger/infixCall.kt");
    }
    
    @Test
    public void testParam1() {
        doTest("testData/debugger/param1.kt");
    }
    
    @Test
    public void testParam2() {
        doTest("testData/debugger/param2.kt");
    }
    
    @Test
    public void testParentesized() {
        doTest("testData/debugger/parentesized.kt");
    }
    
    @Test
    public void testStringTemplate() {
        doTest("testData/debugger/stringTemplate.kt");
    }
    
    @Test
    public void testWhen() {
        doTest("testData/debugger/when.kt");
    }
    
    @Test
    public void testWhile() {
        doTest("testData/debugger/while.kt");
    }
    
    @Test
    public void testPropertyGetter() {
        doTest("testData/debugger/propertyGetter.kt");
    }
}
