package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinConvertToExpressionBodyAssistProposal;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProposal;

import kotlin.jvm.functions.Function1;

public class KotlinConvertToExpressionBodyTestCase extends KotlinSpacesForTabsQuickAssistTestCase<KotlinConvertToExpressionBodyAssistProposal> {
	protected void doTest(String testPath) {
		doTestFor(testPath, new Function1<KotlinEditor, KotlinQuickAssistProposal>() {
            @Override
            public KotlinQuickAssistProposal invoke(KotlinEditor editor) {
                return new KotlinConvertToExpressionBodyAssistProposal(editor);
            }
        });
	}
}
