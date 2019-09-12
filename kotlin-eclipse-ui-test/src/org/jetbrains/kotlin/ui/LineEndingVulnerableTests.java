package org.jetbrains.kotlin.ui;

import org.jetbrains.kotlin.ui.tests.editors.formatter.KotlinIdeaFormatActionTest;
import org.jetbrains.kotlin.ui.tests.editors.quickfix.autoimport.KotlinChangeModifiersQuickFixTest;
import org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions.KotlinConvertToBlockBodyTest;
import org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions.KotlinImplementMethodsTest;
import org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions.KotlinOverrideMethodsTest;
import org.jetbrains.kotlin.ui.tests.refactoring.extract.KotlinExtractVariableTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {
        KotlinChangeModifiersQuickFixTest.class,
        KotlinIdeaFormatActionTest.class,
        KotlinConvertToBlockBodyTest.class,
        KotlinImplementMethodsTest.class,
        KotlinOverrideMethodsTest.class,
        KotlinExtractVariableTest.class
} )
public class LineEndingVulnerableTests {
}
