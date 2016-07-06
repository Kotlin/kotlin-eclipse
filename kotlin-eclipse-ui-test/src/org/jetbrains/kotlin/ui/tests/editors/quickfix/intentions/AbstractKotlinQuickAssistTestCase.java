package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProposal;
import org.junit.Assert;
import org.junit.Before;

import kotlin.jvm.functions.Function1;

public abstract class AbstractKotlinQuickAssistTestCase<Proposal extends KotlinQuickAssistProposal> extends KotlinProjectTestCase {
	@Before
	public void configure() {
		configureProject();
	}
	
	protected void doTestFor(String testPath, Function1<KotlinEditor, KotlinQuickAssistProposal> createProposal) {
		doTestFor(testPath, createProposal, false);
	}
	
	protected void doTestFor(String testPath, 
	        Function1<KotlinEditor, KotlinQuickAssistProposal> createProposal, 
	        boolean joinBuildThread) {
		String fileText = KotlinTestUtils.getText(testPath);
		TextEditorTest testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText);
		KotlinQuickAssistProposal proposal = createProposal.invoke((KotlinEditor) testEditor.getEditor()); 
		
		String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");
        
        String pathToExpectedFile = testPath + ".after";
        File expectedFile = new File(pathToExpectedFile);
        if (!expectedFile.exists()) isApplicableExpected = false;
        
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
				assertByEditor(testEditor.getEditor(), KotlinTestUtils.getText(pathToExpectedFile));
			}
		}
	}
	
	protected abstract void assertByEditor(JavaEditor editor, String expected);
}
