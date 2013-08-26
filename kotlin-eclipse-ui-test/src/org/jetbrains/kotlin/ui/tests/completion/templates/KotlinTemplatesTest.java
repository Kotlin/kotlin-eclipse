package org.jetbrains.kotlin.ui.tests.completion.templates;

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
				
				Separator.SPACE, 2);
				
	}
}
