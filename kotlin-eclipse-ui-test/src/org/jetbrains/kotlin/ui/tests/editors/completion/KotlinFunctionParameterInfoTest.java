package org.jetbrains.kotlin.ui.tests.editors.completion;

import org.junit.Test;


public class KotlinFunctionParameterInfoTest extends KotlinFunctionParameterInfoTestCase {
    @Test
    public void testDeprecated() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/Deprecated.kt");
    }

    @Test
    public void testExtensionOnClassObject() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/ExtensionOnClassObject.kt");
    }

    @Test
    public void testInheritedFunctions() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/InheritedFunctions.kt");
    }

    @Test
    public void testInheritedWithCurrentFunctions() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/InheritedWithCurrentFunctions.kt");
    }

    @Test
    public void testNoAnnotations() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/NoAnnotations.kt");
    }

    @Test
    public void testNotAccessible() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/NotAccessible.kt");
    }

    @Test
    public void testNotGreen() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/NotGreen.kt");
    }

    @Test
    public void testNullableTypeCall() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/NullableTypeCall.kt");
    }

    @Test
    public void testPrintln() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/Println.kt");
    }

    @Test
    public void testSimple() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/Simple.kt");
    }

    @Test
    public void testSimpleConstructor() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/SimpleConstructor.kt");
    }

    @Test
    public void testSuperConstructorCall() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/SuperConstructorCall.kt");
    }

    @Test
    public void testTwoFunctions() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/TwoFunctions.kt");
    }

    @Test
    public void testTwoFunctionsGrey() throws Exception {
        doTest("common_testData/ide/parameterInfo/functionParameterInfo/TwoFunctionsGrey.kt");
    }
}
