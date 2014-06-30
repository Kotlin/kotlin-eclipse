/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.tests.editors;

import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.junit.Assert;
import org.junit.Test;

public class KotlinEditorBaseTest extends KotlinEditorTestCase {
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
		Assert.assertNotNull(textEditor.createEditor("Test.kt", ""));
	}
	
	@Test
	public void getJavaProjectTest() {
		TextEditorTest textEditor = new TextEditorTest();
		Assert.assertNotNull(textEditor.getTestJavaProject());
	}
	
	@Test
	public void createTestProjectTest() {
		TestJavaProject testProject = new TestJavaProject("test_project");
		Assert.assertNotNull(testProject);
	}
}