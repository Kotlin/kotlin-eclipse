/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
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
