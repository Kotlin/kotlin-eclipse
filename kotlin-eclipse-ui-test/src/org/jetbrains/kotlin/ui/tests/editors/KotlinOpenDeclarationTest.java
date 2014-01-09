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

public class KotlinOpenDeclarationTest extends KotlinOpenDeclarationTestCase {

	@Test
	public void navigateToKotlinMethodInOneFile() {
		doTest("Test.kt", 
				"fun test1() = te<caret>st2()<br>" +
				"fun test2() {}", 
				
				"fun test1() = test2()<br>" +
				"fun <caret>test2() {}"); 
	}
	
	@Test
	public void navigateToKotlinMethodWithDefinedSignature() {
		doTest("Test.kt",
				"fun test1(x : Int) = te<caret>st2(x)<br>" +
				"fun test2() {}<br>" +
				"fun test2(a : Int) = {}", 
				
				"fun test1(x : Int) = test2(x)<br>" +
				"fun test2() {}<br>" +
				"fun <caret>test2(a : Int) = {}");
	}
	
	@Test
	public void navigateToJavaClass() {
		doTest("Test.kt",
				"package testing<br>" +
				"fun test1() = So<caret>me()",
				
				"Some.java",
				"package testing;<br>" +
				"public class Some<caret>() { }");
	}
	
	@Test
	public void navigateToJavaMethod() {
		doTest("Test.kt",
				"package testing<br>" +
				"fun test1() {<br>" +
				"var a = Some1()<br>" +
				"a.test<caret>Method()<br>" +
				"}",
				
				"Some1.java",
				"package testing;<br>" +
				"public class Some1 {<br>" +
				"public void testMethod<caret>() {}<br>" +
				"}");
				
	}
	
	@Test
	public void navigateToJavaStaticMethod() {
		doTest("Test.kt",
				"package testing<br>" +
				"fun test1() = Some2.sta<caret>ticMethod()",
				
				"Some2.java",
				"package testing;<br>" +
				"public class Some2 {<br>" +
				"public static void staticMethod<caret>() {}<br>" +
				"}");
	}
}
