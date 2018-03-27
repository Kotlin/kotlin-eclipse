package org.jetbrains.kotlin.ui.tests.scripts.completion;

import org.jetbrains.kotlin.ui.tests.editors.completion.KotlinBasicCompletionTestCase;
import org.junit.Ignore;
import org.junit.Test;


@Ignore("Script support will be fixed in future releases")
public class CompletionInScriptsTest extends KotlinBasicCompletionTestCase {
    @Test
    public void testArgsCompletion() {
        doTest("testData/completion/basic/scripts/argsCompletion.kts");
    }
    
    @Test
    public void testFunctionFromStdlib() {
        doTest("testData/completion/basic/scripts/functionFromStdlib.kts");
    }
    
    @Test
    public void testTypeFromRuntime() {
        doTest("testData/completion/basic/scripts/typeFromRuntime.kts");
    }
    
    @Test
    public void testAbsentMainTemplate() {
        doTest("testData/completion/basic/scripts/absentMainTemplate.kts");
    }
    
    @Test
    public void testKeywordsCompletion() {
        doTest("testData/completion/basic/scripts/keywordsCompletion.kts");
    }
    
    @Test
    public void testLocalDeclarations() {
        doTest("testData/completion/basic/scripts/localDeclarations.kts");
    }
    
    @Test
    public void testClassFromJRE() {
        doTest("testData/completion/basic/scripts/classFromJRE.kts");
    }
}
