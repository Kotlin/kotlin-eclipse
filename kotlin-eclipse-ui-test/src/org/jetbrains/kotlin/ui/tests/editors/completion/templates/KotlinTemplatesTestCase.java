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
package org.jetbrains.kotlin.ui.tests.editors.completion.templates;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils.Separator;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.jetbrains.kotlin.ui.tests.editors.completion.CompletionTestUtilsKt;
import org.junit.Assert;
import org.junit.Before;

public abstract class KotlinTemplatesTestCase extends KotlinProjectTestCase {
	@Before
	public void configure() {
		configureProject();
	}

	public void doTest(String input) {
		doTest(input, input);
	}
	
	public void doTest(String input, String expected) {
		doTest(input, expected, Separator.TAB, 4);
	}
	
	public void doTest(String input, String expected, Separator separator, int spacesCount) {
		configureIndents(separator, spacesCount);

		input = KotlinTestUtils.resolveTestTags(input);
		expected = KotlinTestUtils.resolveTestTags(expected);
		TextEditorTest testEditor = configureEditor("Test.kt", input);
		
		KotlinFileEditor editor = (KotlinFileEditor) testEditor.getEditor();
		ICompletionProposal[] proposals = CompletionTestUtilsKt.getCompletionProposals(editor);
		
		if (!input.equals(expected)) {
			Assert.assertTrue(proposals.length > 0);
	
			applyTemplateProposal((TemplateProposal)proposals[0], editor);
		} else {
			Assert.assertTrue(proposals.length == 0);
		}
			
		EditorTestUtils.assertByEditor(editor, expected);
	}
	
	private void applyTemplateProposal(TemplateProposal templateProposal, JavaEditor editor) {
		templateProposal.apply(editor.getViewer(), (char) 0, 0, KotlinTestUtils.getCaret(editor));
		
		Point point = templateProposal.getSelection(null);
		editor.getViewer().getTextWidget().setCaretOffset(point.x);
	}
	
	private void configureIndents(Separator separator, int spacesCount) {
		EditorsUI.getPreferenceStore().setValue(
				AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, 
				(Separator.SPACE == separator));
		EditorsUI.getPreferenceStore().setValue(
				AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, 
				spacesCount);	
	}
}
