package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.jetbrains.kotlin.ui.editors.quickassist.KotlinConvertToExpressionBodyAssistProposal;

public class KotlinConvertToExpressionBodyTestCase extends KotlinSpacesForTabsQuickAssistTestCase<KotlinConvertToExpressionBodyAssistProposal> {
	protected void doTest(String testPath) {
		doTestFor(testPath, new KotlinConvertToExpressionBodyAssistProposal());
	}
}
