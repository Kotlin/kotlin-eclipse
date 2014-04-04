package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProposal;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinSpecifyTypeAssistProposal;
import org.junit.Assert;

public class KotlinSpecifyTypeTestCase extends KotlinEditorTestCase {

	protected void doTest(String testPath) {
		doTestFor(testPath, new KotlinSpecifyTypeAssistProposal());
	}
	
	private void doTestFor(String testPath, KotlinQuickAssistProposal proposal) {
		String fileText = getText(testPath);
		testEditor = configureEditor(getNameByPath(testPath), fileText);
		
		joinBuildThread();
		
		String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");

        Assert.assertTrue(
                "isAvailable() for " + proposal.getClass() + " should return " + isApplicableExpected,
                isApplicableExpected == proposal.isApplicable());
		
		if (isApplicableExpected) {
			proposal.apply(testEditor.getEditor().getViewer().getDocument());
	
			String pathToExpectedFile = testPath + ".after";
			EditorTestUtils.assertByEditor(testEditor.getEditor(), getText(pathToExpectedFile));
		}
	}
}
