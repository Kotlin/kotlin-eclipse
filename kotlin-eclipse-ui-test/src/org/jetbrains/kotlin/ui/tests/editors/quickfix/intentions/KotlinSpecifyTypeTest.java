package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;

public class KotlinSpecifyTypeTest extends KotlinSpecifyTypeTestCase {

	@Test
	public void testBadCaretPosition() {
		doTest("testData/intentions/specifyType/BadCaretPosition.kt");
	}
	
	@Test
	public void testClassNameClashing() {
		doTest("testData/intentions/specifyType/ClassNameClashing.kt");
	}
	
	@Test
	public void testEnumType() {
		doTest("testData/intentions/specifyType/EnumType.kt");
	}
	
	@Test
	public void testFunctionType() {
		doTest("testData/intentions/specifyType/FunctionType.kt");
	}
	
	@Test
	public void testLoopParameter() {
		doTest("testData/intentions/specifyType/LoopParameter.kt");
	}
	
	@Test
	public void testOnType() {
		doTest("testData/intentions/specifyType/OnType.kt");
	}
	
	@Test
	public void testPublicMember() {
		doTest("testData/intentions/specifyType/PublicMember.kt");
	}
	
	@Test
	public void testRemoveUnresolvedType() {
		doTest("testData/intentions/specifyType/RemoveUnresolvedType.kt");
	}
	
	@Test
	public void testStringRedefined() {
		doTest("testData/intentions/specifyType/StringRedefined.kt");
	}
	
	@Test
	public void testTypeAlreadyProvided() {
		doTest("testData/intentions/specifyType/TypeAlreadyProvided.kt");
	}
	
	@Test
	public void testUnitType() {
		doTest("testData/intentions/specifyType/UnitType.kt");
	}
	
	@Test
	public void testUnknownType() {
		doTest("testData/intentions/specifyType/UnknownType.kt");
	}
}
