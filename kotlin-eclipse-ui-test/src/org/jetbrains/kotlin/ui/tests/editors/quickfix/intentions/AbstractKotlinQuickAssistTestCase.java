package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProposal;
import org.junit.Assert;
import org.junit.Before;

public abstract class AbstractKotlinQuickAssistTestCase<Proposal extends KotlinQuickAssistProposal> extends KotlinProjectTestCase {
	@Before
	public void configure() {
		configureProject();
	}
	
	protected void doTestFor(String testPath, Proposal proposal) {
		doTestFor(testPath, proposal, false);
	}
	
	protected void doTestFor(String testPath, Proposal proposal, boolean joinBuildThread) {
		String fileText = KotlinTestUtils.getText(testPath);
		TextEditorTest testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText);
		
		String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");
        
        if (joinBuildThread) {
        	KotlinTestUtils.joinBuildThread();
        }

        Assert.assertTrue(
                "isAvailable() for " + proposal.getClass() + " should return " + isApplicableExpected,
                isApplicableExpected == proposal.isApplicable());
		
		String shouldFailString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "SHOULD_FAIL_WITH: ");
		
		if (isApplicableExpected) {
			proposal.apply(testEditor.getEditor().getViewer().getDocument());
	
			if (shouldFailString == null) {
				String pathToExpectedFile = testPath + ".after";
				assertByEditor(testEditor.getEditor(), KotlinTestUtils.getText(pathToExpectedFile));
			}
		}
	}
	
	protected abstract void assertByEditor(JavaEditor editor, String expected);
}
