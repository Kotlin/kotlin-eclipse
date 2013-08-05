package org.jetbrains.kotlin.ui.tests.editors;

import junit.framework.Assert;

import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.junit.Test;
import org.jetbrains.kotlin.testframework.editor.*;
import org.jetbrains.kotlin.testframework.utils.*;

public class KotlinEditorBaseTest {
	@Test
	public void sampleTest() {
		KotlinEditor kotlinEditor = new KotlinEditor();
		Assert.assertNotNull(kotlinEditor);
	}
	
	@Test
	public void TextEditorTest() {
		TextEditorTest textEditor = new TextEditorTest();
		Assert.assertNotNull(textEditor);
	}
	
	@Test
	public void openEditorTest() {
		TextEditorTest textEditor = new TextEditorTest();
		try {
			Assert.assertNotNull(textEditor.createEditor("Test.kt", ""));
		} finally {
			textEditor.deleteEditingFile();
		}
	}
	
	@Test
	public void getJavaProjectTest() {
		TextEditorTest textEditor = new TextEditorTest();
		Assert.assertNotNull(textEditor.getJavaProject());
	}
	
	@Test
	public void createTestProjectTest() {
		TestJavaProject testProject = new TestJavaProject("test_project");
		Assert.assertNotNull(testProject);
	}
}