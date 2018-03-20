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
package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;
            
public class KotlinReplaceGetIntentionTest extends KotlinReplaceGetIntentionTestCase {
    
    private static final String PATH_PREFIX = "common_testData/ide/inspectionsLocal/conventionNameCalls/replaceGetOrSet";
    
	@Test
	public void testAcceptableVararg() {
		doTest(PATH_PREFIX + "/acceptableVararg.kt");
	}

	@Test
    public void testArgumentAndFunction() throws Exception {
        doTest(PATH_PREFIX + "/argumentAndFunction.kt");
    }
    
	@Test
    public void testDuplicateArguments() throws Exception {
        doTest(PATH_PREFIX + "/duplicateArguments.kt");
    }
    
	@Test
    public void testExtensionFunction() throws Exception {
        doTest(PATH_PREFIX + "/extensionFunction.kt");
    }
    
	@Test
    public void testFunctionalArgument() throws Exception {
        doTest(PATH_PREFIX + "/functionalArgument.kt");
    }
    
	@Test
    public void testInvalidArgument() throws Exception {
        doTest(PATH_PREFIX + "/invalidArgument.kt");
    }
    
	@Test
    public void testMissingDefaultArgument() throws Exception {
        doTest(PATH_PREFIX + "/missingDefaultArgument.kt");
    }
    
	@Test
    public void testMultiArgument() throws Exception {
        doTest(PATH_PREFIX + "/multiArgument.kt");
    }
    
	@Test
    public void testNoArgument() throws Exception {
        doTest(PATH_PREFIX + "/noArgument.kt");
    }
	
	@Test
    public void testQualifier() throws Exception {
        doTest(PATH_PREFIX + "/qualifier.kt");
    }
    
	@Test
    public void testSanityCheck() throws Exception {
        doTest(PATH_PREFIX + "/sanityCheck.kt");
    }
    
	@Test
    public void testSingleArgument() throws Exception {
        doTest(PATH_PREFIX + "/singleArgument.kt");
    }
    
	@Test
    public void testSuper() throws Exception {
        doTest(PATH_PREFIX + "/super.kt");
    }
	
	@Test
    public void testTopLevelFun() throws Exception {
        doTest(PATH_PREFIX + "/topLevelFun.kt");
    }
    
	@Test
    public void testUnacceptableVararg() throws Exception {
        doTest(PATH_PREFIX + "/unacceptableVararg.kt");
    }
    
	@Test
    public void testUnnamedAndNamed() throws Exception {
        doTest(PATH_PREFIX + "/unnamedAndNamed.kt");
    }
	
	@Test
    public void testReplaceGetInScript() throws Exception {
        doTest("testData/intentions/replaceGetOrSet/replaceGetInScript.kts");
    }
}
