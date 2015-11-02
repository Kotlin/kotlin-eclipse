package org.jetbrains.kotlin.ui.tests.editors.completion;

import org.junit.Ignore;
import org.junit.Test;


public class KotlinFunctionParameterInfoTest extends KotlinFunctionParameterInfoTestCase {
    @Test
    public void testDeprecated() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/Deprecated.kt");
    }

    @Test
    public void testExtensionOnClassObject() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/ExtensionOnClassObject.kt");
    }

    @Test
    public void testInheritedFunctions() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/InheritedFunctions.kt");
    }

    @Test
    public void testInheritedWithCurrentFunctions() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/InheritedWithCurrentFunctions.kt");
    }

    @Test
    public void testNoAnnotations() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/NoAnnotations.kt");
    }

    @Test
    public void testNotAccessible() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/NotAccessible.kt");
    }

    @Test
    public void testNotGreen() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/NotGreen.kt");
    }

    @Test
    public void testNullableTypeCall() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/NullableTypeCall.kt");
    }

    @Test
    public void testPrintln() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/Println.kt");
    }

    @Test
    public void testSimple() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/Simple.kt");
    }

    @Test
    public void testSimpleConstructor() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/SimpleConstructor.kt");
    }

    @Ignore("Unignore after fix bug with visibility")
    @Test
    public void testSuperConstructorCall() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/SuperConstructorCall.kt");
    }

    @Test
    public void testTwoFunctions() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/TwoFunctions.kt");
    }

    @Test
    public void testTwoFunctionsGrey() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionCall/TwoFunctionsGrey.kt");
    }
}
