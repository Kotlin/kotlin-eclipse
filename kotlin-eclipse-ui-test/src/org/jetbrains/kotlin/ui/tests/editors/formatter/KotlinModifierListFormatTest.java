package org.jetbrains.kotlin.ui.tests.editors.formatter;

import org.junit.Test;

public class KotlinModifierListFormatTest extends KotlinFormatActionTestCase {
    @Override
    protected AfterSuffixPosition getAfterPosition() {
        return AfterSuffixPosition.BEFORE_DOT;
    }

    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/formatter/modifierList";
    }
    
    @Test
    public void funAnnotationBeforeAnnotation() {
        doAutoTest();
    }

    @Test
    public void funAnnotationBeforeAnnotationEntry() {
        doAutoTest();
    }

    @Test
    public void funAnnotationBeforeModifiers() {
        doAutoTest();
    }

    @Test
    public void funAnnotationEntryBeforeAnnotation() {
        doAutoTest();
    }

    @Test
    public void funAnnotationEntryBeforeAnnotationEntry() {
        doAutoTest();
    }

    @Test
    public void funAnnotationEntryBeforeModifiers() {
        doAutoTest();
    }

    @Test
    public void funModifierBeforeAnnotation() {
        doAutoTest();
    }

    @Test
    public void funModifierBeforeAnnotationEntry() {
        doAutoTest();
    }

    @Test
    public void funModifierBeforeModifiers() {
        doAutoTest();
    }

    @Test
    public void funTheOnlyModifier() {
        doAutoTest();
    }

    @Test
    public void memberFunTheOnlyModifier() {
        doAutoTest();
    }

    @Test
    public void memberValTheOnlyModifier() {
        doAutoTest();
    }

    @Test
    public void memberVarTheOnlyModifier() {
        doAutoTest();
    }

    @Test
    public void secondMemberFunTheOnlyModifier() {
        doAutoTest();
    }

    @Test
    public void secondMemberValTheOnlyModifier() {
        doAutoTest();
    }

    @Test
    public void secondMemberVarTheOnlyModifier() {
        doAutoTest();
    }
}
