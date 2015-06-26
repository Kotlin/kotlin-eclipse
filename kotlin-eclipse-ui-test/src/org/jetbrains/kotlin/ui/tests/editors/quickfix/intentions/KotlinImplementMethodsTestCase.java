package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinImplementMethodsProposal;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProposal;
import org.junit.Assert;
import org.junit.Before;

public class KotlinImplementMethodsTestCase extends KotlinProjectTestCase {
	@Before
	public void configure() {
		configureProject();
		EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, true);
	}
	
	protected void doTest(String testPath) {
		doTestFor(testPath, new KotlinImplementMethodsProposal());
	}
	
	private void doTestFor(String testPath, KotlinQuickAssistProposal proposal) {
		String fileText = KotlinTestUtils.getText(testPath);
		TextEditorTest testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText);
		
		String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");

        Assert.assertTrue(
                "isAvailable() for " + proposal.getClass() + " should return " + isApplicableExpected,
                isApplicableExpected == proposal.isApplicable());
		
		if (isApplicableExpected) {
			proposal.apply(testEditor.getEditor().getViewer().getDocument());
	
			String pathToExpectedFile = testPath + ".after";
			String expectedText = removeSelectionAndCaretTags(KotlinTestUtils.getText(pathToExpectedFile));
			EditorTestUtils.assertByEditor(testEditor.getEditor(), expectedText);
		}
	}
	
	private String removeSelectionAndCaretTags(String text) {
		return text
				.replaceAll("<selection>", "")
				.replaceAll("</selection>", "")
				.replaceAll("<caret>", "")
				.replaceAll("</caret>", "");
	}
}
