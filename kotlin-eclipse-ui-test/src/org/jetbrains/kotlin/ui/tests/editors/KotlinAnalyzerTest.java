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

import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.junit.Ignore;
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
			"<error>prin</error>(\"Hello\")<br>" +
			"}", "Test2.kt");
	}
	
	@Test
	@Ignore("Temporary disable external annotations")
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
