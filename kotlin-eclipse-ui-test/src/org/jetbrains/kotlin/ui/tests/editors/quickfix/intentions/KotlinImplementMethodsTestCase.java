package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinImplementMethodsProposal;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProposal;

import kotlin.jvm.functions.Function1;

public class KotlinImplementMethodsTestCase extends KotlinSpacesForTabsQuickAssistTestCase<KotlinImplementMethodsProposal> {
	protected void doTest(String testPath) {
		String exceptionCall = "TODO(\"not implemented\") //To change body of created functions use File | Settings | File Templates.";
        doTestFor(testPath, new Function1<KotlinEditor, KotlinQuickAssistProposal>() {
            @Override
            public KotlinQuickAssistProposal invoke(KotlinEditor editor) {
                return new KotlinImplementMethodsProposal(editor, exceptionCall);
            }
        });
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
