package org.jetbrains.kotlin.ui.tests.editors;

import org.junit.Test;

public class KotlinSelectEnclosingTest extends KotlinSelectEnclosingTestCase {
	
	@Test
	public void selectEnclosingFunctionNameWithoutSelection() {
		doAutoTest();
	}
}
