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
	
   @Test
    public void insertClosingBraceOnLastLine() {
        doTest("fun foo() {\n\tFoo()\n<caret>", '}', "fun foo() {\n\tFoo()\n}");
    }
	
	@Test
	public void insertClosingBrace() {
	    doTest("fun foo() {\n\tFoo()\n<caret>\nclass Foo", '}', "fun foo() {\n\tFoo()\n}\nclass Foo");
	}
	
	@Test
	public void insertClosingBraceWithRemovingTabulation() {
	    doTest("class Foo {\n\tfun foo() {\n\t\tFoo()\n\t\t<caret>\n}", '}', "class Foo {\n\tfun foo() {\n\t\tFoo()\n\t}\n}");
	}
}
