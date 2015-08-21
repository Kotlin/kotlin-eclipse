/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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

import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.junit.Test;

/**
 * Tests for bug <a href="https://youtrack.jetbrains.com/issue/KT-8193">KT-8193</a>.
 */
public class KotlinEditorClosedProjectInfluenceTest extends KotlinEditorTestCase {

	private static final String PROJECT_NAME_FIRST = "!FirstProject";
	private static final String PROJECT_NAME_SECOND = "SecondProject";

	@Test
	public void testTwoProjectsBothOpen() throws CoreException {
		new TestJavaProject(PROJECT_NAME_FIRST);
		TestJavaProject secondProject = new TestJavaProject(PROJECT_NAME_SECOND);

		IFile file = secondProject.createSourceFile(
			TextEditorTest.TEST_PACKAGE_NAME, "Test.kt", "// Lorem ipsum"
		);
		IEditorPart editor = EditorTestUtils.openInEditor(file);

		assertTrue(editor instanceof KotlinFileEditor);
	}

	@Test
	public void testTwoProjectsOneClosed() throws CoreException {
		TestJavaProject firstProject = new TestJavaProject(PROJECT_NAME_FIRST);
		TestJavaProject secondProject = new TestJavaProject(PROJECT_NAME_SECOND);

		firstProject.getJavaProject().getProject().close(null);

		IFile file = secondProject.createSourceFile(
			TextEditorTest.TEST_PACKAGE_NAME, "Test.kt", "// Lorem ipsum"
		);
		IEditorPart editor = EditorTestUtils.openInEditor(file);

		assertTrue(editor instanceof KotlinFileEditor);
	}

}
