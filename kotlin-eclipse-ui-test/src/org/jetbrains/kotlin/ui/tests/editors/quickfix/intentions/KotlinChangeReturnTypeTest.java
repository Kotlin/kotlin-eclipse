package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinChangeReturnTypeTest extends KotlinChangeReturnTypeTestCase {
    @Test
    public void testNonLocalReturnRuntime() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/nonLocalReturnRuntime.kt");
    }
    
    @Test
    public void testChangeFunctionReturnTypeToFunctionType() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/changeFunctionReturnTypeToFunctionType.kt");
    }
    
    @Test
    public void testNonLocalReturnWithLabelRuntime() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/nonLocalReturnWithLabelRuntime.kt");
    }
    
    @Test
    public void testTypeMismatchInIfStatementReturnedByFunction() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/typeMismatchInIfStatementReturnedByFunction.kt");
    }
    
    @Test
    public void testTypeMismatchInInitializer() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/typeMismatchInInitializer.kt");
    }
    
    @Test
    public void testTypeMismatchInReturnStatement() {
        doTest("common_testData/ide/quickfix/typeMismatch/typeMismatchOnReturnedExpression/typeMismatchInReturnStatement.kt");
    }
    
    @Test
    public void testTypeMismatchInReturnLambda() {
        doTest("testData/intentions/changeReturnType/typeMismatchInReturnLambda.kt");
    }
    
    @Test
    public void testTypeMismatchInReturnLambdaWithLabel() {
        doTest("testData/intentions/changeReturnType/typeMismatchInReturnLambdaWithLabel.kt");
    }
    

    @Ignore("Script support will be fixed in future releases")
    @Test
    public void testChangeReturnTypeInScript() {
        doTest("testData/intentions/changeReturnType/changeReturnTypeInScript.kts");
    }
}