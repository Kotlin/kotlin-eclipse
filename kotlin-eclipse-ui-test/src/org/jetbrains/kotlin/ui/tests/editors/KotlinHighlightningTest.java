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
package org.jetbrains.kotlin.ui.tests.editors;

import org.junit.Test;

public class KotlinHighlightningTest extends KotlinHighlightningTestCase {

	@Test
	public void oneKeywordWithoutTextIn() {
		doTest("<keyword>in</keyword>");
	}
	
	@Test
	public void oneKeywordWithoutTextFor() {
		doTest("<keyword>for</keyword>");
	}
	
	@Test
	public void oneKeywordWithText() {
		doTest("<keyword>in</keyword> sdfsd");
	}
	
	@Test
	public void defaultTokenBeforeKeyword() {
		doTest("abcd <keyword>for</keyword>");
	}
	
	@Test
	public void defaultTokenWithKeywordInSuffix() {
		doTest("main");
	}
	
	@Test
	public void defaultTokenWithKeywordInPrefix() {
		doTest("inprefix");
	}
	
	@Test
	public void defaultTokenWithKeywordInMiddle() {
		doTest("sinus");
	}
	
	@Test
	public void stringToken() {
		doTest("<string>\"Hello\"</string>");
	}
	
	@Test
	public void blockCommentToken() {
		doTest(
				"<comment>/*test<br>" +
				"test<br>" +
				"*/</comment>");
	}
	
	@Test
	public void singleLineCommentToken() {
		doTest("<comment>// for main</comment>");
	}
	
	@Test
	public void highlightImportKeyword() {
		doTest("<keyword>import</keyword> some");
	}
	
	@Test
	public void highlightOpenKeyword() {
		doTest("<keyword>open</keyword> <keyword>class</keyword> T");
	}
	
	@Test
	public void doNotHightlightImportKeywordInBody() {
		doTest("<keyword>class</keyword> T { import }");
	}
	
	@Test
	public void highlightSoftKeywords() {
		doTest(
				"<keyword>import</keyword> some<br>" +
				"<keyword>open</keyword> <keyword>abstract</keyword> <keyword>class</keyword> T { import }");
	}
}
