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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.junit.Test;

public class PsiVisualizationCommandTest extends CommandTestCase {

	public static final String COMMAND_ID_PSI_VISUALIZATION = "org.jetbrains.kotlin.psi.visualization";	

	@Test
	public void testPsiVisualizationCommandWithoutEditor() {
		assertCommandEnabled(COMMAND_ID_PSI_VISUALIZATION, false);
	}

	@Test
	public void testPsiVisualizationCommandInKotlinEditor() {
		configureEditor("Test.kt", "// Lorem ipsum");

		assertCommandEnabled(COMMAND_ID_PSI_VISUALIZATION, true);
	}

	@Test
	public void testPsiVisualizationCommandInJavaEditor() throws CoreException {
		TextEditorTest testEditor = new TextEditorTest();

		IFile file = testEditor.getTestJavaProject().createSourceFile(
			TextEditorTest.TEST_PACKAGE_NAME, "Test.java", "// Lorem ipsum"
		);
		EditorTestUtils.openInEditor(file);

		assertCommandEnabled(COMMAND_ID_PSI_VISUALIZATION, false);
	}

}
