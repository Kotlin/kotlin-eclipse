package org.jetbrains.kotlin.ui.tests.editors.organizeImports;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinCommonOptimizeImportsTest extends KotlinOrganizeImportsTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/editor/optimizeImports/common";
    }
    
    @Ignore("Enable this test when autoimport for functions will be ready")
    @Test
    public void ArrayAccessExpression() {
        doAutoTest();
    }
    
    @Ignore
    @Test
    public void ClassMemberImported() {
        doAutoTest();
    }
    
    @Ignore("Enable this test when autoimport for functions will be ready")
    @Test
    public void ComponentFunction() {
        doAutoTest();
    }
    
    @Test
    public void CurrentPackage() {
        doAutoTest();
    }
    
    @Test
    public void DefaultObjectReference() {
        doAutoTest();
    }
    
    @Ignore
    @Test
    public void Enums() {
        doAutoTest();
    }
    
    @Ignore
    @Test
    public void InvokeFunction() {
        doAutoTest();
    }
    
    @Ignore("Enable this test when autoimport for functions will be ready")
    @Test
    public void IteratorFunction() {
        doAutoTest();
    }
    
    @Test
    public void KeywordNames() {
        doAutoTest();
    }
    
    @Test
    public void Kt2488EnumEntry() {
        doAutoTest();
    }
    
    @Test
    public void Kt2709() {
        doAutoTest();
    }
    
    @Test
    public void KT9875() {
        doAutoTest();
    }
    
    @Test
    public void MembersInScope() {
        doAutoTest();
    }
    
    @Test
    public void NestedClassReferenceOutsideClassBody() {
        doAutoTest();
    }
}
