package org.jetbrains.kotlin.ui.tests.editors.organizeImports;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinJvmOptimizeImportsTest extends KotlinOrganizeImportsTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/editor/optimizeImports/jvm";
    }
    
    @Test
    public void AlreadyOptimized() {
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
    public void DoNotTouchIfNoChanges() {
        doAutoTest();
    }
    
    @Test
    public void DuplicatedImports() {
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
    public void NestedClassInObject() {
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
    
    @Test
    public void UnusedImports() {
        doAutoTest();
    }
    
    @Test
    public void WithAliases() {
        doAutoTest();
    }
}
