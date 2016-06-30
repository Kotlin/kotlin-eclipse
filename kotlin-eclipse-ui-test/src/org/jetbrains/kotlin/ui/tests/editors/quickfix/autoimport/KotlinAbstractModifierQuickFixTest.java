package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport;

import org.junit.Test;

public class KotlinAbstractModifierQuickFixTest extends KotlinQuickFixTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/quickfix/abstract/";
    }
    
    @Test
    public void abstractFunctionInNonAbstractClass() {
        doAutoTest();
    }
    
    @Test
    public void abstractPropertyInNonAbstractClass1() {
        doAutoTest();
    }
    
    @Test
    public void abstractPropertyInNonAbstractClass2() {
        doAutoTest();
    }
    
    @Test
    public void abstractPropertyInNonAbstractClass3() {
        doAutoTest();
    }
    
    @Test
    public void abstractPropertyInPrimaryConstructorParameters() {
        doAutoTest();
    }
    
    @Test
    public void abstractPropertyNotInClass() {
        doAutoTest();
    }
    
    @Test
    public void abstractPropertyWithGetter1() {
        doAutoTest();
    }
    
    @Test
    public void abstractPropertyWithInitializer1() {
        doAutoTest();
    }
    
    @Test
    public void abstractPropertyWithSetter() {
        doAutoTest();
    }
    
    @Test
    public void mustBeInitializedOrBeAbstract() {
        doAutoTest();
    }
    
    @Test
    public void nonMemberAbstractFunction() {
        doAutoTest();
    }
    
    @Test
    public void notImplementedMember() {
        doAutoTest();
    }
    
    @Test
    public void notImplementedMemberFromAbstractClass() {
        doAutoTest();
    }
    
    @Test
    public void replaceOpen() {
        doAutoTest();
    }
}
