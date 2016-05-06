package org.jetbrains.kotlin.ui.tests.editors.organizeImports;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinOptimizeImportsTest extends KotlinOrganizeImportsTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/editor/optimizeImports";
    }
    
    @Test
    public void AlreadyOptimized() {
        doAutoTest();
    }
    
    @Ignore("Enable this test when autoimport for functions will be ready")
    @Test
    public void ArrayAccessExpression() {
        doAutoTest();
    }
    
    @Test
    public void CallableReference() {
        doAutoTest();
    }
    
    @Test
    public void ClassFromSameFileImportAddedBug() {
        doAutoTest();
    }
    
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
    public void DefaultJsImports() {
        doAutoTest();
    }
    
    @Test
    public void DefaultObjectReference() {
        doAutoTest();
    }
    
    @Test
    public void DoNotTouchIfNoChanges() {
        doAutoTest();
    }
    
    @Test
    public void DuplicatedImports() {
        doAutoTest();
    }
    
    @Test
    public void Enums() {
        doAutoTest();
    }
    
    @Test
    public void FromCompanionObject() {
        doAutoTest();
    }
    
    @Test
    public void FromCompanionObjectGeneric() {
        doAutoTest();
    }
    
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
    public void KT10226() {
        doAutoTest();
    }
    
    @Test
    public void Kt1850FullQualified() {
        doAutoTest();
    }
    
    @Test
    public void Kt1850InnerClass() {
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
    public void NestedClassInObject() {
        doAutoTest();
    }
    
    @Test
    public void NestedClassReferenceOutsideClassBody() {
        doAutoTest();
    }
    
    @Test
    public void RemoveImportsIfGeneral() {
        doAutoTest();
    }
    
    @Test
    public void RemoveImportsIfGeneralBefore() {
        doAutoTest();
    }
    
    @Test
    public void SamConstructor() {
        doAutoTest();
    }
    
    @Test
    public void StaticMethodFromSuper() {
        doAutoTest();
    }
    
    @Test
    public void ThisAndSuper() {
        doAutoTest();
    }
    
    @Test
    public void TrivialAlias() {
        doAutoTest();
    }
    
    @Ignore("Enable this test when autoimport for functions will be ready")
    @Test
    public void UnusedImports() {
        doAutoTest();
    }
    
    @Test
    public void WithAliases() {
        doAutoTest();
    }
}
