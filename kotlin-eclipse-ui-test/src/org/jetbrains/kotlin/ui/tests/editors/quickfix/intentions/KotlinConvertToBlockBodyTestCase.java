package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinConvertToBlockBodyAssistProposal;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProposal;

import kotlin.jvm.functions.Function1;

public class KotlinConvertToBlockBodyTestCase extends
		KotlinSpacesForTabsQuickAssistTestCase<KotlinConvertToBlockBodyAssistProposal> {

	protected void doTest(String testPath) {
		doTestFor(testPath, new Function1<KotlinEditor, KotlinQuickAssistProposal>() {
            @Override
            public KotlinQuickAssistProposal invoke(KotlinEditor editor) {
                return new KotlinConvertToBlockBodyAssistProposal(editor);
            }
        });
	}
}
