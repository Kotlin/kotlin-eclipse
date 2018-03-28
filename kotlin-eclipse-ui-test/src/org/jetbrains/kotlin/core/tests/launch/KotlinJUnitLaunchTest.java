package org.jetbrains.kotlin.core.tests.launch;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class KotlinJUnitLaunchTest extends KotlinJUnitLaunchTestCase {
	@Test
	public void testSimpleJUnitTests() {
		doTest("testData/launch/junit/SimpleJUnitTests.kt");
	}
	
	@Test
	public void testRunTestExtendingTestCase() {
		doTest("testData/launch/junit/RunTestExtendingTestCase.kt");
	}
}
