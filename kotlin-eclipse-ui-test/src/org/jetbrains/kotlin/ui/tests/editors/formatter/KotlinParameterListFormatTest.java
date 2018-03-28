package org.jetbrains.kotlin.ui.tests.editors.formatter;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class KotlinParameterListFormatTest extends KotlinFormatActionTestCase {
    @Override
    protected AfterSuffixPosition getAfterPosition() {
        return AfterSuffixPosition.BEFORE_DOT;
    }

    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/formatter/parameterList";
    }
    
    @Test
    public void ArgumentListChopAsNeeded() {
        doAutoTest();
    }

    @Test
    public void ArgumentListDoNotWrap() {
        doAutoTest();
    }

    @Test
    public void ArgumentListWrapAlways() {
        doAutoTest();
    }

    @Test
    public void ArgumentListWrapAsNeeded() {
        doAutoTest();
    }

    @Test
    public void ParameterListChopAsNeeded() {
        doAutoTest();
    }

    @Test
    public void ParameterListDoNotWrap() {
        doAutoTest();
    }

    @Test
    public void ParameterListWrapAlways() {
        doAutoTest();
    }

    @Test
    public void ParameterListWrapAsNeeded() {
        doAutoTest();
    }
}
