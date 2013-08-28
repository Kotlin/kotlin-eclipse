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
