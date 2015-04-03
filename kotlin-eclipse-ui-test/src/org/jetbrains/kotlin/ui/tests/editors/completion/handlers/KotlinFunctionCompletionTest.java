package org.jetbrains.kotlin.ui.tests.editors.completion.handlers;

import org.junit.Test;

public class KotlinFunctionCompletionTest extends KotlinFunctionCompletionTestCase {
	@Override
	protected String getTestDataRelativePath() {
		return "completion/handlers";
	}
	
	@Test
	public void ClassCompletionInImport() {
		doAutoTest();
	}
	
	@Test
	public void ClassCompletionInLambda() {
		doAutoTest();
	}
	
	@Test
	public void ClassCompletionInMiddle() {
		doAutoTest();
	}
	
	@Test
	public void ClassFromClassObject() {
		doAutoTest();
	}
	
	@Test
	public void ClassFromClassObjectInPackage() {
		doAutoTest();
	}
	
	@Test
	public void DoNotInsertImportForAlreadyImported() {
		doAutoTest();
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
	public void HigherOrderFunctionWithArgs2() {
		doAutoTest();
	}
	
	@Test
	public void HigherOrderFunctionWithArgs3() {
		doAutoTest();
	}
	
	@Test
	public void ImportedEnumMember() {
		doAutoTest();
	}
}
