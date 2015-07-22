package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;

public class KotlinConvertToBlockBodyTest extends
		KotlinConvertToBlockBodyTestCase {
	@Test
	public void testAddSpace() {
		doTest("common_testData/ide/intentions/convertToBlockBody/addSpace.kt");
	}

	@Test
	public void testAnnotatedExpr() {
		doTest("common_testData/ide/intentions/convertToBlockBody/annotatedExpr.kt");
	}

	@Test
	public void testExplicitlyNonUnitFun() {
		doTest("common_testData/ide/intentions/convertToBlockBody/explicitlyNonUnitFun.kt");
	}

	@Test
	public void testExplicitlyTypedFunWithUnresolvedExpression() {
		doTest("common_testData/ide/intentions/convertToBlockBody/explicitlyTypedFunWithUnresolvedExpression.kt");
	}

	@Test
	public void testExplicitlyTypedFunWithUnresolvedType() {
		doTest("common_testData/ide/intentions/convertToBlockBody/explicitlyTypedFunWithUnresolvedType.kt");
	}

	@Test
	public void testExplicitlyUnitFun() {
		doTest("common_testData/ide/intentions/convertToBlockBody/explicitlyUnitFun.kt");
	}

	@Test
	public void testExplicitlyUnitFunWithUnresolvedExpression() {
		doTest("common_testData/ide/intentions/convertToBlockBody/explicitlyUnitFunWithUnresolvedExpression.kt");
	}

	@Test
	public void testFunWithThrow() {
		doTest("common_testData/ide/intentions/convertToBlockBody/funWithThrow.kt");
	}

	@Test
	public void testGetter() {
		doTest("common_testData/ide/intentions/convertToBlockBody/getter.kt");
	}

	@Test
	public void testGetterWithThrow() {
		doTest("common_testData/ide/intentions/convertToBlockBody/getterWithThrow.kt");
	}

	@Test
	public void testImplicitlyTypedFunWithUnresolvedType() {
		doTest("common_testData/ide/intentions/convertToBlockBody/implicitlyTypedFunWithUnresolvedType.kt");
	}

	@Test
	public void testImplicitlyUnitFun() {
		doTest("common_testData/ide/intentions/convertToBlockBody/implicitlyUnitFun.kt");
	}

	@Test
	public void testLabeledExpr() {
		doTest("common_testData/ide/intentions/convertToBlockBody/labeledExpr.kt");
	}

	@Test
	public void testNothingFun() {
		doTest("common_testData/ide/intentions/convertToBlockBody/nothingFun.kt");
	}

	@Test
	public void testSetter() {
		doTest("common_testData/ide/intentions/convertToBlockBody/setter.kt");
	}
}
