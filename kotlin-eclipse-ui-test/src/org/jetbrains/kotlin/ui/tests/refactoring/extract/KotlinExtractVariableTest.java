package org.jetbrains.kotlin.ui.tests.refactoring.extract;

import org.junit.Test;

public class KotlinExtractVariableTest extends KotlinExtractVariableTestCase {
    @Test
    public void testIfCondition() {
        doTest("common_testData/ide/refactoring/introduceVariable/IfCondition.kt", "b");
    }
    
    @Test
    public void testArrayAccess() {
        doTest("common_testData/ide/refactoring/introduceVariable/ArrayAccessExpr.kt");
    }
    
    @Test
    public void testCallUnderSmartCast() {
        doTest("common_testData/ide/refactoring/introduceVariable/callUnderSmartCast.kt", "foo");
    }
    
    @Test
    public void testComplexCallee() {
        doTest("common_testData/ide/refactoring/introduceVariable/ComplexCallee.kt", "function");
    }
    
    @Test
    public void testDelegatorByExpressionInDelegate() {
        doTest("common_testData/ide/refactoring/introduceVariable/DelegatorByExpressionInDelegate.kt", "o");
    }
    
    @Test
    public void testDelegatorToSuperCallInArgument() {
        doTest("common_testData/ide/refactoring/introduceVariable/DelegatorToSuperCallInArgument.kt");
    }
    
    @Test
    public void testDoWhileAddBlock() {
        doTest("common_testData/ide/refactoring/introduceVariable/DoWhileAddBlock.kt");
    }
    
    @Test
    public void testDoWhileAddBlockInner() {
        doTest("common_testData/ide/refactoring/introduceVariable/DoWhileAddBlockInner.kt");
    }
    
    @Test
    public void testFewOccurrences() {
        doTest("common_testData/ide/refactoring/introduceVariable/FewOccurrences.kt");
    }
    
    @Test
    public void testFunctionAddBlock() {
        doTest("common_testData/ide/refactoring/introduceVariable/FunctionAddBlock.kt");
    }
    
    @Test
    public void testFunctionAddBlockInner() {
        doTest("common_testData/ide/refactoring/introduceVariable/FunctionAddBlockInner.kt");
    }
    
    @Test
    public void testIfElseAddBlock() {
        doTest("common_testData/ide/refactoring/introduceVariable/IfElseAddBlock.kt");
    }
    
    @Test
    public void testIfThenAddBlock() {
        doTest("common_testData/ide/refactoring/introduceVariable/IfCondition.kt");
    }
    
    @Test
    public void testIfThenAddBlockInner() {
        doTest("common_testData/ide/refactoring/introduceVariable/IfThenAddBlockInner.kt");
    }
    
    @Test
    public void testIfThenValuedAddBlock() {
        doTest("common_testData/ide/refactoring/introduceVariable/IfThenValuedAddBlock.kt");
    }
    
    @Test
    public void testIntroduceAndCreateBlock() {
        doTest("common_testData/ide/refactoring/introduceVariable/IntroduceAndCreateBlock.kt");
    }
    
    @Test
    public void testManyInnerOccurences() {
        doTest("common_testData/ide/refactoring/introduceVariable/ManyInnerOccurences.kt");
    }
    
    @Test
    public void testManyOccurrences() {
        doTest("common_testData/ide/refactoring/introduceVariable/ManyOccurrences.kt");
    }
    
    @Test
    public void testNoNewLinesInBetween() {
        doTest("common_testData/ide/refactoring/introduceVariable/NoNewLinesInBetween.kt", "bar");
    }
    
    @Test
    public void testNoNewLinesInBetweenNoBraces() {
        doTest("common_testData/ide/refactoring/introduceVariable/NoNewLinesInBetweenNoBraces.kt", "bar");
    }
    
    @Test
    public void testNotNullAssertion() {
        doTest("common_testData/ide/refactoring/introduceVariable/notNullAssertion.kt", "length");
    }
    
    @Test
    public void testOccurrencesInStringTemplate() {
        doTest("common_testData/ide/refactoring/introduceVariable/OccurrencesInStringTemplate.kt");
    }
    
    @Test
    public void testOneExplicitReceiver() {
        doTest("common_testData/ide/refactoring/introduceVariable/OneExplicitReceiver.kt", "prop");
    }
    
    @Test
    public void testPropertyAccessorAddBlock() {
        doTest("common_testData/ide/refactoring/introduceVariable/PropertyAccessorAddBlock.kt");
    }
    
    @Test
    public void testPropertyAccessorAddBlockInner() {
        doTest("common_testData/ide/refactoring/introduceVariable/PropertyAccessorAddBlockInner.kt");
    }
    
    @Test
    public void testReplaceOccurence() {
        doTest("common_testData/ide/refactoring/introduceVariable/ReplaceOccurence.kt");
    }
    
    @Test
    public void testSimple() {
        doTest("common_testData/ide/refactoring/introduceVariable/Simple.kt");
    }
    
    @Test
    public void testSimpleCreateValue() {
        doTest("common_testData/ide/refactoring/introduceVariable/SimpleCreateValue.kt");
    }
    
    @Test
    public void testLoopRange() {
        doTest("common_testData/ide/refactoring/introduceVariable/LoopRange.kt", "intRange");
    }
    
    @Test
    public void testThisReference() {
        doTest("common_testData/ide/refactoring/introduceVariable/ThisReference.kt", "a");
    }
    
    @Test
    public void testTwoExplicitReceivers() {
        doTest("common_testData/ide/refactoring/introduceVariable/TwoExplicitReceivers.kt", "f1");
    }
    
    @Test
    public void testWhenAddBlock() {
        doTest("common_testData/ide/refactoring/introduceVariable/WhenAddBlock.kt");
    }
    
    @Test
    public void testWhenAddBlockInner() {
        doTest("common_testData/ide/refactoring/introduceVariable/WhenAddBlockInner.kt");
    }
    
    @Test
    public void testWhileCondition() {
        doTest("common_testData/ide/refactoring/introduceVariable/WhileCondition.kt", "b");
    }
}
