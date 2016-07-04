package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;

public class KotlinImplementMethodsTest extends KotlinImplementMethodsTestCase {
	@Test
	public void testDefaultValues() {
		doTest("common_testData/ide/codeInsight/overrideImplement/defaultValues.kt");
	}
	
	@Test
	public void testEmptyClassBodyFunctionMethod() {
		doTest("common_testData/ide/codeInsight/overrideImplement/emptyClassBodyFunctionMethod.kt");
	}
	
	@Test
	public void testFunctionMethod() {
		doTest("common_testData/ide/codeInsight/overrideImplement/functionMethod.kt");
	}
	
	@Test
	public void testFunctionProperty() {
		doTest("common_testData/ide/codeInsight/overrideImplement/functionProperty.kt");
	}
	
	@Test
	public void testFunctionWithTypeParameters() {
		doTest("common_testData/ide/codeInsight/overrideImplement/functionWithTypeParameters.kt");
	}
	
	@Test
	public void testGenericMethod() {
		doTest("common_testData/ide/codeInsight/overrideImplement/genericMethod.kt");
	}
	
	@Test
	public void testGenericTypesSeveralMethods() {
		doTest("common_testData/ide/codeInsight/overrideImplement/genericTypesSeveralMethods.kt");
	}
	
	@Test
	public void testLocalClass() {
		doTest("common_testData/ide/codeInsight/overrideImplement/localClass.kt");
	}
	
	@Test
	public void testOverrideExplicitFunction() {
		doTest("common_testData/ide/codeInsight/overrideImplement/overrideExplicitFunction.kt");
	}
	
	@Test
	public void testOverrideExtensionProperty() {
		doTest("common_testData/ide/codeInsight/overrideImplement/overrideExtensionProperty.kt");
	}
	
	@Test
	public void testOverrideMutableExtensionProperty() {
		doTest("common_testData/ide/codeInsight/overrideImplement/overrideMutableExtensionProperty.kt");
	}
	
	@Test
	public void testProperty() {
		doTest("common_testData/ide/codeInsight/overrideImplement/property.kt");
	}
	
	@Test
	public void testTraitGenericImplement() {
		doTest("common_testData/ide/codeInsight/overrideImplement/traitGenericImplement.kt");
	}
	
	@Test
	public void testTraitNullableFunction() {
		doTest("common_testData/ide/codeInsight/overrideImplement/traitNullableFunction.kt");
	}
	
	@Test
    public void testImplementMethodInScript() {
        doTest("testData/intentions/implementMethods/implementMethodInScript.kts");
    }
}
