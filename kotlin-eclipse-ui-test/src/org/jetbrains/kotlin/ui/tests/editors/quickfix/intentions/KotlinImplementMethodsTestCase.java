package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinImplementMethodsProposal;

public class KotlinImplementMethodsTestCase extends KotlinSpacesForTabsQuickAssistTestCase<KotlinImplementMethodsProposal> {
	protected void doTest(String testPath) {
		doTestFor(testPath, new KotlinImplementMethodsProposal());
	}

	@Override
	protected void assertByEditor(JavaEditor editor, String expected) {
		EditorTestUtils.assertByEditor(editor, removeCaretAndSelection(expected));
	}
	
	private String removeCaretAndSelection(String text) {
		return text
				.replaceAll("<caret>", "")
				.replaceAll("<selection>", "")
				.replaceAll("</selection>", "");
	}
}
