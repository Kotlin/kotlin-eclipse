package org.jetbrains.kotlin.ui.tests.completion.templates;

import junit.framework.Assert;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.ui.editors.codeassist.CompletionProcessor;

public class KotlinTemplatesTestCase extends KotlinEditorTestCase {
	
	public void doTest(String input, String expected) {
		testEditor = configureEditor("Test.kt", input);
		
		CompletionProcessor ktCompletionProcessor = new CompletionProcessor();
		ICompletionProposal[] proposals = ktCompletionProcessor.computeCompletionProposals(testEditor.getEditor().getViewer(), getCaret());
		
		Assert.assertTrue(proposals.length > 0);

		applyTemplateProposal((TemplateProposal)proposals[0]);
		
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
	
	@Override
	protected TextEditorTest configureEditor(String fileName, String content) {
		EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, false);
		
		return super.configureEditor(fileName, content);
	}
}
