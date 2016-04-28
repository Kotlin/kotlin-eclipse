package org.jetbrains.kotlin.ui.tests.editors.completion.handlers;

import org.junit.Test;

public class KotlinCompletionHandlerInsertTest extends KotlinCompletionHandlerInsertTestCase {
	@Override
	protected String getTestDataRelativePath() {
		return "completion/handlers";
	}
	
	@Test
	public void existingSingleBrackets() {
		doAutoTest();
	}

	@Test
	public void functionLiteralInsertOnSpace() {
		doAutoTest();
	}
	
	@Test
	public void higherOrderFunction() {
		doAutoTest();
	}
	
	@Test
	public void higherOrderFunctionWithArgs1() {
		doAutoTest();
	}
	
	@Test
 	public void insertFunctionWithBothParentheses() {
 		doAutoTest();
 	}
	
	@Test
	public void insertFunctionWithSingleParameterWithBrace() {
		doAutoTest();
	}
	
	@Test
	public void insertVoidJavaMethod() {
		doAutoTest();
	}
	
	@Test
	public void noParamsFunction() {
		doAutoTest();
	}
	
	@Test
	public void paramsFunction() {
		doAutoTest();
	}
	
	@Test
	public void singleBrackets() {
		doAutoTest();
	}
	
	@Test
	public void insertJavaMethodWithParam() {
		doAutoTest();
	}
	
	@Test
	public void functionWithParamOnBracket() {
		doAutoTest();
	}
	
	@Test
	public void paramFunctionOnBracket() {
		doAutoTest();
	}
	
	@Test
	public void unitFunctionOnBracket() {
		doAutoTest();
	}
	
	@Test
	public void unitFunctionOnDot() {
		doAutoTest();
	}
	
	@Test
	public void functionWithParamOnDot() {
		doAutoTest();
	}
	
	@Test
	public void paramFunctionOnDot() {
		doAutoTest();
	}
	
	@Test
	public void paramsFunctionOnDot() {
		doAutoTest();
	}
	
	@Test
	public void completeWithExistingBraces() {
		doAutoTest();
	}
	
	@Test
	public void completeWithExistingBracesOnBrace() {
		doAutoTest();
	}
	
	@Test
	public void completeWithExistingBracesOnDot() {
		doAutoTest();
	}
	
	@Test
	public void withParamsAndBraces() {
		doAutoTest();
	}
	
	@Test
	public void withParamsAndBracesOnBrace() {
		doAutoTest();
	}
	
	@Test
	public void withParamsAndBracesOnDot() {
		doAutoTest();
	}
	
	@Test
	public void withLambdaAndBraces() {
		doAutoTest();
	}
	
	@Test
	public void withLambdaAndBracesOnDot() {
		doAutoTest();
	}
	
	@Test
    public void completeNonImported() {
        doAutoTest();
    }
	
	@Test
    public void nonImportedByCamelCase() {
        doAutoTest();
    }
}