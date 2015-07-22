package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.jetbrains.kotlin.ui.editors.quickassist.KotlinConvertToBlockBodyAssistProposal;

public class KotlinConvertToBlockBodyTestCase extends
		KotlinSpacesForTabsQuickAssistTestCase<KotlinConvertToBlockBodyAssistProposal> {

	protected void doTest(String testPath) {
		doTestFor(testPath, new KotlinConvertToBlockBodyAssistProposal());
	}
}
