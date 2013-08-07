package org.jetbrains.kotlin.ui.tests.editors;

import junit.framework.Assert;

public class KotlinBracketInserterTestCase extends KotlinEditorTestCase {

	protected void doTest(String input, char element, String expected) {
		testEditor = configureEditor("Test.kt", input);
		
		testEditor.type(element);
		
		String actual = testEditor.getEditorInput();
		
		Assert.assertEquals(expected, actual);
	}
}
