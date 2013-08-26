package org.jetbrains.kotlin.ui.tests.completion.templates;

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
	
	private int getCaret() {
		return testEditor.getEditor().getViewer().getTextWidget().getCaretOffset();
	}
	
	private void configureIndents(Separator separator, int spacesCount) {
		EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, (Separator.SPACE == separator));
		EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, spacesCount);	
	}
}
