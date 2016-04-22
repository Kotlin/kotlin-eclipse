package org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinChangeModifiersQuickFixTest extends KotlinQuickFixTestCase {
    
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/quickfix/modifiers/";
    }
    
    @Test
    public void abstractModifierInEnum() {
        doAutoTest();
    }
    
    @Test
    public void finalTrait() {
        doAutoTest();
    }
    
    @Test
    public void illegalEnumAnnotation1() {
        doAutoTest();
    }
    
    @Test
    public void illegalEnumAnnotation2() {
        doAutoTest();
    }
    
    @Test
    public void infixModifier() {
        doAutoTest();
    }
    
    @Test
    public void kt10409() {
        doAutoTest();
    }
    
    @Test
    public void nestedClassNotAllowed() {
        doAutoTest();
    }
    
    @Test
    public void openMemberInFinalClass1() {
        doAutoTest();
    }
    
    @Test
    public void openMemberInFinalClass2() {
        doAutoTest();
    }
    
    @Test
    public void openMemberInFinalClass3() {
        doAutoTest();
    }
    
    @Test
    public void openMemberInFinalClass4() {
        doAutoTest();
    }
    
    @Test
    public void openModifierInEnum() {
        doAutoTest();
    }
    
    @Test
    public void operatorModifier() {
        doAutoTest();
    }
    
    @Test
    public void operatorModifierCollection() {
        doAutoTest();
    }
    
    @Test
    public void operatorModifierComponent() {
        doAutoTest();
    }
    
    @Test
    public void operatorModifierGet() {
        doAutoTest();
    }
    
    @Test
    public void operatorModifierInherited() {
        doAutoTest();
    }
    
    @Test
    public void packageMemberCannotBeProtected() {
        doAutoTest();
    }
    
    @Ignore("Temporary ignore this")
    @Test
    public void removeAbstractModifier() {
        doAutoTest();
    }
    
    @Test
    public void removeIncompatibleModifier() {
        doAutoTest();
    }
    
    @Test
    public void removeInnerForClassInTrait() {
        doAutoTest();
    }
    
    @Test
    public void removeProtectedModifier() {
        doAutoTest();
    }
    
    @Test
    public void removeRedundantModifier1() {
        doAutoTest();
    }
    
    @Test
    public void removeRedundantModifier2() {
        doAutoTest();
    }
    
    @Test
    public void removeRedundantModifier3() {
        doAutoTest();
    }
}
