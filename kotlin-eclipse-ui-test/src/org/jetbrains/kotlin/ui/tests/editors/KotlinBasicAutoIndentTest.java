/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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

public class KotlinBasicAutoIndentTest extends KotlinAutoIndentTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "format/autoIndent";
    }
    
    @Override
    protected AfterSuffixPosition getAfterPosition() {
        return AfterSuffixPosition.BEFORE_DOT;
    }
	
	@Test
	public void sampleTest() {
	    doAutoTest();
	}
	
	@Test
	public void betweenBracesOnOneLine() {
	    doAutoTest();
	}
	
	@Test
	public void betweenBracesOnDifferentLine() {
	    doAutoTest();
	}
	
	@Test
	public void beforeCloseBrace() {
	    doAutoTest();
	}
	
	@Test
	public void afterOneOpenBrace() {
	    doAutoTest();
	}
	
	@Test
	public void lineBreakSaveIndent() {
	    doAutoTest();
	}
	
	@Test
	public void afterOperatorIfWithoutBraces() {
	    doAutoTest();
	}
	
	@Test
	public void afterOperatorWhileWithoutBraces() {
	    doAutoTest();
	}
	
	
	@Test
	public void breakLineAfterIfWithoutBraces() {
	    doAutoTest();
	}
	
	@Test
	public void nestedOperatorsWithBraces() {
	    doAutoTest();
	}
	
	@Test
	public void nestedOperatorsWithoutBraces() {
	    doAutoTest();
	}
	
	@Test
	public void beforeFunctionStart() {
	    doAutoTest();
	}
}
