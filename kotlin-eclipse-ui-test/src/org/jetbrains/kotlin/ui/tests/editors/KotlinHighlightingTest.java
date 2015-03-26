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

public class KotlinHighlightingTest extends KotlinHighlightingTestCase {
	
	@Override
    protected String getTestDataRelativePath() {
        return "highlighting/basic";
    }
	
	@Test
	public void kdocBasic() {
		doAutoTest();
	}
	
	@Test
	public void kdocWithParam() {
		doAutoTest();
	}
	
	@Test
	public void kdocWithProperty() {
		doAutoTest();
	}
	
	@Test
	public void kdocWithSee() {
		doAutoTest();
	}
	
	@Test
	public void kdocWithThrows() {
		doAutoTest();
	}
	
	@Test
	public void kdocWithEmptyLines() {
		doAutoTest();
	}
	
	@Test
	public void kdocWithMyTag() {
		doAutoTest();
	}
	
	@Test
	public void kdocWithoutLeadingAsterisk() {
		doAutoTest();
	}
	
	@Test
	public void function() {
		doAutoTest();
	}
	
	@Test
	public void getterSetter() {
		doAutoTest();
	}
	
	@Test
	public void highlightCompanionObject() {
		doAutoTest();
	}
	
	@Test
	public void trait() {
		doAutoTest();
	}
	
	@Test
	public void blockComment() {
		doAutoTest();
	}
	
	@Test
	public void forKeyword() {
		doAutoTest();
	}
	
	@Test
	public void importKeyword() {
		doAutoTest();
	}
	
	@Test
	public void inKeyword() {
		doAutoTest();
	}
	
	@Test
	public void keywordWithText() {
		doAutoTest();
	}
	
	@Test
	public void openKeyword() {
		doAutoTest();
	}

	@Test
	public void singleLineComment() {
		doAutoTest();
	}
	
	@Test
	public void softImportKeyword() {
		doAutoTest();
	}
	
	@Test
	public void softKeywords() {
		doAutoTest();
	}
	
	@Test
	public void stringInterpolation() {
		doAutoTest();
	}
	
	@Test
	public void textWithTokenBetween() {
		doAutoTest();
	}
	
	@Test
	public void textWithTokenInPrefix() {
		doAutoTest();
	}
	
	@Test
	public void textWithTokenInSuffix() {
		doAutoTest();
	}
}
