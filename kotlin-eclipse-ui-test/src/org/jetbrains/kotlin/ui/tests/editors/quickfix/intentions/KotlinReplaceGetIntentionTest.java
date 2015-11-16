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
	
	@Test
	public void testAcceptableVararg() {
		doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/acceptableVararg.kt");
	}

	@Test
    public void testArgumentAndFunction() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/argumentAndFunction.kt");
    }
    
	@Test
    public void testDuplicateArguments() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/duplicateArguments.kt");
    }
    
	@Test
    public void testExtensionFunction() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/extensionFunction.kt");
    }
    
	@Test
    public void testFunctionalArgument() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/functionalArgument.kt");
    }
    
	@Test
    public void testInvalidArgument() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/invalidArgument.kt");
    }
    
	@Test
    public void testMissingDefaultArgument() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/missingDefaultArgument.kt");
    }
    
	@Test
    public void testMultiArgument() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/multiArgument.kt");
    }
    
	@Test
    public void testNoArgument() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/noArgument.kt");
    }
	
	@Test
    public void testQualifier() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/qualifier.kt");
    }
    
	@Test
    public void testSanityCheck() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/sanityCheck.kt");
    }
    
	@Test
    public void testSingleArgument() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/singleArgument.kt");
    }
    
	@Test
    public void testSuper() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/super.kt");
    }
	
	@Test
    public void testTopLevelFun() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/topLevelFun.kt");
    }
    
	@Test
    public void testUnacceptableVararg() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/unacceptableVararg.kt");
    }
    
	@Test
    public void testUnnamedAndNamed() throws Exception {
        doTest("common_testData/ide/intentions/conventionNameCalls/replaceGetOrSet/unnamedAndNamed.kt");
    }
}
