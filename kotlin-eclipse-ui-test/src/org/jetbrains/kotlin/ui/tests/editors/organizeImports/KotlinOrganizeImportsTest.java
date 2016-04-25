package org.jetbrains.kotlin.ui.tests.editors.organizeImports;

import org.junit.Test;

public class KotlinOrganizeImportsTest extends KotlinOrganizeImportsTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "organizeImports";
    }
    
    @Test
    public void includeImportsOnlyfromActiveFile() {
        doAutoTest();
    }
    
    @Test
    public void doNotInsertLinesForNoErrors() {
        doAutoTest();
    }
    
    @Test
    public void importOneClass() {
        doAutoTest();
    }
    
    @Test
    public void importSeveralClasses() {
        doAutoTest();
    }
    
    @Test
    public void importSeveralClassesWithExistingPackage() {
        doAutoTest();
    }
}
