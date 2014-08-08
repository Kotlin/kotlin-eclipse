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
package org.jetbrains.kotlin.ui.tests.editors.completion.templates;

import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.junit.Test;

public class KotlinTemplatesTest extends KotlinTemplatesTestCase {

	@Test
	public void completeMainTemplate() {
		doTest("main<caret>", 
				"fun main(args : Array<String>) {<br>" +
				"\t<caret><br>" +
				"}");
	}
	
	@Test
	public void completeMainTemplateAfterText() {
		doTest(
				"// comment<br>" +
				"mai<caret>",
				
				"// comment<br>" +
				"fun main(args : Array<String>) {<br>" +
				"\t<caret><br>" +
				"}");
	}
	
	@Test
	public void doNotCompleteMainInClass() {
		doTest(
				"class Test {<br>" +
				"\tmain<caret><br>" +
				"}");
	}
	
	@Test
	public void doNotCompleteMainInDeclaration() {
		doTest("fun test() = main<caret>");
	}
	
	@Test
	public void doNotCompleteMainInExpression() {
		doTest("fun test() = if (main<caret>) {}");
	}
	
	@Test
	public void completeMainTemplateUsingSpacesInsteadTab() {
		doTest(
				"main<caret>",
				
				"fun main(args : Array<String>) {<br>" +
				"  <caret><br>" +
				"}", 
				
				KotlinTestUtils.Separator.SPACE, 2);
				
	}
}