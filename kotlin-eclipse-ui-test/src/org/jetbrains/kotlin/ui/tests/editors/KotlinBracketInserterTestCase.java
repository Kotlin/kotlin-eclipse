package org.jetbrains.kotlin.ui.tests.editors;

import junit.framework.Assert;

import org.jetbrains.kotlin.testframework.editor.TextEditorTest;

public class KotlinBracketInserterTestCase {

	protected void doTest(String input, char element, String expected) {
		TextEditorTest testEditor = null;
		try {
			testEditor = new TextEditorTest();
			testEditor.createEditor("Test.kt", input);
			
			testEditor.type(element);
			
			String actual = testEditor.getEditorInput();
			
			Assert.assertEquals(expected, actual);
		} finally {
			if (testEditor != null) {
				testEditor.close();
			}
		}
	}
}
