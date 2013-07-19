package org.jetbrains.kotlin.ui.tests.editors;

import org.junit.Test;

public class KotlinBracketInserterTest extends KotlinBracketInserterTestCase {

	@Test
	public void simpleBracketAutoCompletion() {
		doTest("<caret>", '(', "()");
	}
	
	@Test
	public void insertBeforeOpenBracket() {
		doTest("println<caret>()", '(', "println(()");
	}
	
	@Test
	public void insertBeforeBetweenBracket() {
		doTest("(<caret>)", '(', "(())");
	}
	
	@Test
	public void insertAngleBracket() {
		doTest("<caret>", '<', "<>");
	}
	
	@Test
	public void insertBrace() {
		doTest("<caret>", '{', "{}");
	}
	
	@Test
	public void insertBracketInsideString() {
		doTest("\"Hel<caret>lo\"", '(', "\"Hel(lo\"");
	}
}
