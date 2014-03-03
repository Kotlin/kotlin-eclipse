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

import junit.framework.Assert;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.ui.editors.codeassist.CompletionProcessor;

public class KotlinTemplatesTestCase extends KotlinEditorTestCase {
	
	public void doTest(String input) {
		doTest(input, input);
	}
	
	public void doTest(String input, String expected) {
		doTest(input, expected, Separator.TAB, 0);
	}
	
	public void doTest(String input, String expected, Separator separator, int spacesCount) {
		configureIndents(separator, spacesCount);
		
		testEditor = configureEditor("Test.kt", input);
		
		joinBuildThread();
		
		CompletionProcessor ktCompletionProcessor = new CompletionProcessor(testEditor.getEditor());
		ICompletionProposal[] proposals = ktCompletionProcessor.computeCompletionProposals(testEditor.getEditor().getViewer(), getCaret());
		
		if (!input.equals(expected)) {
			Assert.assertTrue(proposals.length > 0);
	
			applyTemplateProposal((TemplateProposal)proposals[0]);
		} else {
			Assert.assertTrue(proposals.length == 0);
		}
			
		EditorTestUtils.assertByEditor(testEditor.getEditor(), expected);
	}
	
	private void applyTemplateProposal(TemplateProposal templateProposal) {
		templateProposal.apply(testEditor.getEditor().getViewer(), (char) 0, 0, getCaret());
		
		Point point = templateProposal.getSelection(null);
		testEditor.getEditor().getViewer().getTextWidget().setCaretOffset(point.x);
	}
	
	private void configureIndents(Separator separator, int spacesCount) {
		EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, (Separator.SPACE == separator));
		EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, spacesCount);	
	}
}
