package org.jetbrains.kotlin.ui.tests.editors.formatter;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    KotlinFormatActionTest.class,
    KotlinIdeaFormatActionTest.class,
    KotlinFileAnnotationsFormatTest.class,
    KotlinModifierListFormatTest.class,
    KotlinParameterListFormatTest.class
})
public class AllFormatTests {

}
