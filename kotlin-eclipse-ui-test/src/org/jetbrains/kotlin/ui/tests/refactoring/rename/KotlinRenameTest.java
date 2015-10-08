package org.jetbrains.kotlin.ui.tests.refactoring.rename;

import org.junit.Test;

public class KotlinRenameTest extends KotlinRenameTestCase {
    @Test
    public void testSimple() {
        doTest("testData/refactoring/rename/simple/info.test");
    }
    
    @Test
    public void testAutomaticRenamer() {
        doTest("testData/refactoring/rename/automaticRenamer/simple.test");
    }
}