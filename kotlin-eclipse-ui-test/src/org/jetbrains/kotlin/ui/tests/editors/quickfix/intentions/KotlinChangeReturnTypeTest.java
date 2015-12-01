package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;

public class KotlinChangeReturnTypeTest extends KotlinChangeReturnTypeTestCase {
    @Test
    public void testNonLocalReturnRuntime() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/nonLocalReturnRuntime.kt");
    }
    
    @Test
    public void testAssignmentTypeMismatch() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/assignmentTypeMismatch.kt");
    }
    
    @Test
    public void testChangeFunctionReturnTypeToFunctionType() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/changeFunctionReturnTypeToFunctionType.kt");
    }
    
    @Test
    public void testChangeFunctionReturnTypeToMatchReturnTypeOfReturnedLiteral() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/changeFunctionReturnTypeToMatchReturnTypeOfReturnedLiteral.kt");
    }
    
    @Test
    public void testNonLocalReturnWithLabelRuntime() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/nonLocalReturnWithLabelRuntime.kt");
    }
    
    @Test
    public void testPropertyGetterInitializerTypeMismatch() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/propertyGetterInitializerTypeMismatch.kt");
    }
    
    @Test
    public void testReturnedExpressionTypeMismatchFunctionParameterType() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/returnedExpressionTypeMismatchFunctionParameterType.kt");
    }
    
    @Test
    public void testTypeMismatchInIfStatementReturnedByFunction() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/typeMismatchInIfStatementReturnedByFunction.kt");
    }
    
    @Test
    public void testTypeMismatchInIfStatementReturnedByLiteral() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/typeMismatchInIfStatementReturnedByLiteral.kt");
    }
    
    @Test
    public void testTypeMismatchInInitializer() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/typeMismatchInInitializer.kt");
    }
    
    @Test
    public void testTypeMismatchInReturnStatement() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/typeMismatchInReturnStatement.kt");
    }
}
