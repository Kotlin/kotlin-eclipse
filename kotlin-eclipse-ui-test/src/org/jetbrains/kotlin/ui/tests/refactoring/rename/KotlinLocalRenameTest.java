package org.jetbrains.kotlin.ui.tests.refactoring.rename;

import org.junit.Test;

public class KotlinLocalRenameTest extends KotlinLocalRenameTestCase {
    @Test
    public void testForLoop() {
        doTest("common_testData/ide/refactoring/rename/inplace/ForLoop.kt", "j");
    }
    
    @Test
    public void testFunctionLiteral() {
        doTest("common_testData/ide/refactoring/rename/inplace/FunctionLiteral.kt", "y");
    }
    
    @Test
    public void testFunctionLiteralParenthesis() {
        doTest("common_testData/ide/refactoring/rename/inplace/FunctionLiteralParenthesis.kt", "y");
    }
    
    @Test
    public void testLocalFunction() {
        doTest("common_testData/ide/refactoring/rename/inplace/LocalFunction.kt", "bar");
    }
    
    @Test
    public void testMultiDeclaration() {
        doTest("common_testData/ide/refactoring/rename/inplace/MultiDeclaration.kt", "foo");
    }
    
    @Test
    public void testTryCatch() {
        doTest("common_testData/ide/refactoring/rename/inplace/TryCatch.kt", "e1");
    }
}
