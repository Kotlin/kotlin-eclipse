package org.jetbrains.kotlin.ui.tests.editors.completion;

import org.junit.Test;

public class KotlinCompletionRelevanceTest extends KotlinCompletionRelevanceTestCase {
    @Test
    public void testLocalBeforeNonImported() {
        doTest("testData/completion/relevance/localBeforeNonImported.kt");
    }
    
    @Test
    public void testSortingForLocal() {
        doTest("testData/completion/relevance/sortingForLocal.kt");
    }
    
    @Test
    public void testSortingForNonImported() {
        doTest("testData/completion/relevance/sortingForNonImported.kt");
    }
    
    @Test
    public void testByCamelCaseLocal() {
        doTest("testData/completion/relevance/byCamelCaseLocal.kt");
    }
    
    @Test
    public void testByPrefixMatchLocal() {
        doTest("testData/completion/relevance/byPrefixMatchLocal.kt");
    }
    
    @Test
    public void testByPrefixWithImported() {
        doTest("testData/completion/relevance/byPrefixWithImported.kt");
    }
}
