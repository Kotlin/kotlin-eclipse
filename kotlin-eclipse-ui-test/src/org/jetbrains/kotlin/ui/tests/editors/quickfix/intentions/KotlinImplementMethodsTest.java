package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;

public class KotlinImplementMethodsTest extends KotlinImplementMethodsTestCase {
	@Test
	public void testAcceptableVararg() {
		doTest("testData/intentions/implementMethods/complexMultiOverride.kt");
	}
}
