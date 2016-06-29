package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinImplementMethodsProposal;

public class KotlinImplementMethodsTestCase extends KotlinSpacesForTabsQuickAssistTestCase<KotlinImplementMethodsProposal> {
	protected void doTest(String testPath) {
		String exceptionCall = "UnsupportedOperationException(\"not implemented\") //To change body of created functions use File | Settings | File Templates.";
        doTestFor(testPath, new KotlinImplementMethodsProposal(exceptionCall));
	}

	@Override
	protected void assertByEditor(JavaEditor editor, String expected) {
		EditorTestUtils.assertByEditor(editor, removeCaretAndSelection(expected));
	}
	
	public static String removeCaretAndSelection(String text) {
		return text
				.replaceAll("<caret>", "")
				.replaceAll("<selection>", "")
				.replaceAll("</selection>", "");
	}
}
