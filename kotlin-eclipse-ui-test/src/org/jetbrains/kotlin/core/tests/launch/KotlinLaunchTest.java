package org.jetbrains.kotlin.core.tests.launch;

import org.junit.Test;

public class KotlinLaunchTest extends KotlinLaunchTestCase {
	
	private static final String sourceCode = "fun main(args : Array<String>) = {}";

	@Test
	public void launchSimpleProject() {
		doTest(sourceCode, "test_project", "org.jet.pckg", null);
	}
	
	@Test
	public void launchWhenProjectNameHaveSpace() {
		doTest(sourceCode, "test project", "pckg", null);
	}
	
	@Test
	public void launchWithTwoSourceFolders() {
		doTest(sourceCode, "testProject", "pckg", "src2");
	}
	
	@Test
	public void launchWhenSourceFolderHaveSpace() {
		doTest(sourceCode, "testProject", "pckg", "src directory");
	}
}
