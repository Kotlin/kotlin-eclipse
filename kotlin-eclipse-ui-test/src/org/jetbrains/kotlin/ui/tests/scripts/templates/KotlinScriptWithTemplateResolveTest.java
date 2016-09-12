package org.jetbrains.kotlin.ui.tests.scripts.templates;

import org.junit.Test;

public class KotlinScriptWithTemplateResolveTest extends KotlinScriptWithTemplateResolveTestCase {
    @Test
    public void testSample() {
        doTest("testData/scripts/templates/sample.testDef.kts");
    }
    
    @Test
    public void testStandard() {
        doTest("testData/scripts/templates/standard.kts");
    }
    
    @Test
    public void testSampleEx() {
        doTest("testData/scripts/templates/sampleEx.testDef.kts");
    }
    
    @Test
    public void testCustomEPResolver() {
        doTest("testData/scripts/templates/customEPResolver.kts");
    }
}
