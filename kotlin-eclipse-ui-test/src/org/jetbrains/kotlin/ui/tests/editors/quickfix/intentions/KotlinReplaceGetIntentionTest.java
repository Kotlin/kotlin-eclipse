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
		doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/acceptableVararg.kt");
	}

	@Test
    public void testArgumentAndFunction() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/argumentAndFunction.kt");
    }
    
	@Test
    public void testDuplicateArguments() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/duplicateArguments.kt");
    }
    
	@Test
    public void testExtensionFunction() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/extensionFunction.kt");
    }
    
	@Test
    public void testFunctionalArgument() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/functionalArgument.kt");
    }
    
	@Test
    public void testInvalidArgument() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/invalidArgument.kt");
    }
    
	@Test
    public void testMissingArgument() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/missingArgument.kt");
    }
    
	@Test
    public void testMissingDefaultArgument() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/missingDefaultArgument.kt");
    }
    
	@Test
    public void testMultiArgument() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/multiArgument.kt");
    }
    
	@Test
    public void testNamedAndFunction() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/namedAndFunction.kt");
    }
    
	@Test
    public void testNoArgument() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/noArgument.kt");
    }
    
	@Test
    public void testSanityCheck() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/sanityCheck.kt");
    }
    
	@Test
    public void testSingleArgument() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/singleArgument.kt");
    }
    
	@Test
    public void testSingleNamedArgument() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/singleNamedArgument.kt");
    }
    
	@Test
    public void testUnacceptableVararg() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/unacceptableVararg.kt");
    }
    
	@Test
    public void testUnnamedAndNamed() throws Exception {
        doTest("testData/intentions/attributeCallReplacements/replaceGetIntention/unnamedAndNamed.kt");
    }
}
