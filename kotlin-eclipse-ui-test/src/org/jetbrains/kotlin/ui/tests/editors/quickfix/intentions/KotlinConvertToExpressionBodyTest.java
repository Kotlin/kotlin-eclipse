package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;

public class KotlinConvertToExpressionBodyTest extends
		KotlinConvertToExpressionBodyTestCase {
	
    @Test
    public void testAnonymousObjectExpression() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/anonymousObjectExpression.kt");
    }

    @Test
    public void testAssignment() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/assignment.kt");
    }

    @Test
    public void testDeclaration() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/declaration.kt");
    }

    @Test
    public void testExpressionWithReturns1() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/expressionWithReturns1.kt");
    }

    @Test
    public void testExpressionWithReturns2() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/expressionWithReturns2.kt");
    }

    @Test
    public void testFunctionLiteral() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/functionLiteral.kt");
    }

    @Test
    public void testFunWithImplicitUnitTypeWithThrow() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/funWithImplicitUnitTypeWithThrow.kt");
    }

    @Test
    public void testFunWithNoBlock() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/funWithNoBlock.kt");
    }

    @Test
    public void testFunWithNothingType() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/funWithNothingType.kt");
    }

    @Test
    public void testFunWithReturn() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/funWithReturn.kt");
    }

    @Test
    public void testFunWithUnitType2() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/funWithUnitType2.kt");
    }

    @Test
    public void testFunWithUnitType() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/funWithUnitType.kt");
    }

    @Test
    public void testFunWithUnitTypeWithThrow() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/funWithUnitTypeWithThrow.kt");
    }

    @Test
    public void testGetWithReturn() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/getWithReturn.kt");
    }

    @Test
    public void testMultipleStatements() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/multipleStatements.kt");
    }

    @Test
    public void testOverridePublicFun() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/overridePublicFun.kt");
    }

    @Test
    public void testReturnWithNoValue() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/returnWithNoValue.kt");
    }

    @Test
    public void testWhile() {
        doTest("common_testData/ide/intentions/convertToExpressionBody/while.kt");
    }
}
