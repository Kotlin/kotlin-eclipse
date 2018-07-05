package org.jetbrains.kotlin.core.tests.diagnostics;

import org.jetbrains.kotlin.checkers.KotlinDiagnosticsTestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JavaAnnotationArgumentsInKotlinTest extends KotlinDiagnosticsTestCase {
    @Override
    @Before
    public void configure() {
        configureProjectWithStdLib();
    }

    @Test
    public void testAnnotationAsJavaAnnotationArgument() throws Exception {
        // KE-266
        doTest("testData/diagnostics/annotationAsJavaAnnotationArgument.kt");
    }

    @Test
    public void testConstAsJavaAnnotationArgument() throws Exception {
        doTest("testData/diagnostics/constAsJavaAnnotationArgument.kt");
    }
}
