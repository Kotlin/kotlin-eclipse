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
