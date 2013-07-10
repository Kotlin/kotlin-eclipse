package org.jetbrains.kotlin.ui.tests.editors;

import org.junit.Test;

public class KotlinAutoIndenterTest extends KotlinAutoIndenterTestCase {
	
	@Test
	public void sampleTest() {
		doTest("ab<caret>c", "ab\nc");
	}
	
	@Test
	public void betweenBracesOnOneLine() {
		doTest("{<caret>}", 
				
				"{\n" +
				"\t<caret>\n" +
				"}");
	}
	
	@Test
	public void betweenBracesOnDifferentLine() {
		doTest("fun tt() {<caret>\n}",
				
				"fun tt() {\n" +
				"\t<caret>\n" +
				"}");
	}
	
	@Test
	public void beforeCloseBrace() {
		doTest("{\n<caret>}",
				
				"{\n" +
				"\n<caret>" +
				"}");
	}
	
	@Test
	public void afterOneOpenBrace() {
		doTest("fun tt(){<caret>",
				
				"fun tt(){\n" +
				"\t<caret>");
	}
	
	@Test
	public void lineBreakSaveIndent() {
		doTest("fun tt() {\n\t<caret>\n}",
				
				"fun tt() {\n" +
				"\t\n" +
				"\t<caret>\n" +
				"}");
	}
	
	@Test
	public void afterOperatorIfWithoutBraces() {
		doTest("fun tt() {\n" +
				"\tif (true)<caret>\n" +
				"}",
				
				"fun tt() {\n" +
				"\tif (true)\n" +
				"\t\t<caret>\n" +
				"}");
	}
	
	@Test
	public void afterOperatorWhileWithoutBraces() {
		doTest("fun tt() {\n" +
				"\twhile (true)<caret>\n" +
				"}",
				
				"fun tt() {\n" +
				"\twhile (true)\n" +
				"\t\t<caret>\n" +
				"}");
	}
	
	
	@Test
	public void breakLineAfterIfWithoutBraces() {
		doTest("fun tt() {\n" +
				"\tif (true)\n" +
				"\t\ttrue<caret>\n" +
				"}",
				
				"fun tt() {\n" +
				"\tif (true)\n" +
				"\t\ttrue\n" +
				"\t<caret>\n" +
				"}");
	}
	
	@Test
	public void nestedOperatorsWithBraces() {
		doTest("fun tt() {\n" +
				"\tif (true) {\n" +
				"\t\tif (true) {<caret>\n" +
				"\t\t}\n" +
				"\t}\n" +
				"}",
				
				"fun tt() {\n" +
				"\tif (true) {\n" +
				"\t\tif (true) {\n" +
				"\t\t\t<caret>\n" +
				"\t\t}\n" +
				"\t}\n" +
				"}");
	}
	
	@Test
	public void nestedOperatorsWithoutBraces() {
		doTest("fun tt() {\n" +
				"\tif (true)\n" +
				"\t\tif (true)<caret>\n" +
				"}",
				
				"fun tt() {\n" +
				"\tif (true)\n" +
				"\t\tif (true)\n" +
				"\t\t\t<caret>\n" +
				"}");
	}
	
	@Test
	public void typeCloseBraceInEmptyEditor() {
		doTest("}", "}"); // Check that excetion not throwing
	}
}
