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
}
