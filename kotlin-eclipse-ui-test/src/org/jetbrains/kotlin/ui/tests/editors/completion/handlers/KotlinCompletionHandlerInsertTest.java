package org.jetbrains.kotlin.ui.tests.editors.completion.handlers;

import org.junit.Test;

public class KotlinCompletionHandlerInsertTest extends KotlinCompletionHandlerInsertTestCase {
	@Override
	protected String getTestDataRelativePath() {
		return "completion/handlers";
	}
	
	@Test
	public void ExistingSingleBrackets() {
		doAutoTest();
	}

	@Test
	public void FunctionLiteralInsertOnSpace() {
		doAutoTest();
	}
	
	@Test
	public void HigherOrderFunction() {
		doAutoTest();
	}
	
	@Test
	public void HigherOrderFunctionWithArgs1() {
		doAutoTest();
	}
	
	@Test
 	public void InsertFunctionWithBothParentheses() {
 		doAutoTest();
 	}
	
	@Test
	public void InsertFunctionWithSingleParameterWithBrace() {
		doAutoTest();
	}
	
	@Test
	public void InsertVoidJavaMethod() {
		doAutoTest();
	}
	
	@Test
	public void NoParamsFunction() {
		doAutoTest();
	}
	
	@Test
	public void ParamsFunction() {
		doAutoTest();
	}
	
	@Test
	public void SingleBrackets() {
		doAutoTest();
	}
	
	@Test
	public void InsertJavaMethodWithParam() {
		doAutoTest();
	}
	
	@Test
	public void FunctionWithParamOnBracket() {
		doAutoTest();
	}
	
	@Test
	public void ParamFunctionOnBracket() {
		doAutoTest();
	}
	
	@Test
	public void UnitFunctionOnBracket() {
		doAutoTest();
	}
	
	@Test
	public void UnitFunctionOnDot() {
		doAutoTest();
	}
	
	@Test
	public void FunctionWithParamOnDot() {
		doAutoTest();
	}
	
	@Test
	public void ParamFunctionOnDot() {
		doAutoTest();
	}
	
	@Test
	public void ParamsFunctionOnDot() {
		doAutoTest();
	}
}
