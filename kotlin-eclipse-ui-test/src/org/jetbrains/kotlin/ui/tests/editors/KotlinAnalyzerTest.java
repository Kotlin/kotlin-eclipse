package org.jetbrains.kotlin.ui.tests.editors;

import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.junit.Test;

public class KotlinAnalyzerTest extends KotlinAnalyzerTestCase {

	@Test
	public void hasAnalyzerKotlinRuntime() {
		doTest(
			"fun main(args : Array<String>) {<br>" +
			"println(\"Hello\")<br>" +
			"}", "Test1.kt");
	}
	
	@Test
	public void checkAnalyzerFoundError() {
		doTest(
			"fun main(args : Array<String>) {<br>" +
			"<err>prin</err>(\"Hello\")<br>" +
			"}", "Test2.kt");
	}
	
	@Test
	public void hasAnalyzerKotlinAnnotations() {
		doTest(
			"import java.util.regex.Pattern<br>" +
			"fun main(args : Array<String>) {<br>" +
			"  println(Pattern.compile(\"Some\").matcher(\"Some\"))<br>" +
			"}", "Test3.kt");
	}
	
	@Test
	public void useJavaCodeFromKotlinFile() {
		TextEditorTest editorWithJavaCode = configureEditor("Some.java", "package testing; public class Some() { }");
		editorWithJavaCode.save();
		editorWithJavaCode.close();
		
		doTest(
			"package testing<br>" +
			"fun tt() {<br>" +
			"var a = Some()<br>" +
			"}", "Test5.kt");
	}
}
