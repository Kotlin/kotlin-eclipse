package org.jetbrains.kotlin.core.tests.diagnostics;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class KotlinDiagnosticsJavaPlusKotlinTest extends KotlinDiagnosticsTestCase {
	@Override
	@Before
	public void configure() {
		configureProjectWithStdLib();
	}
	
	@Test
	public void testresolveConstructor() throws Exception {
		doTest("testData/diagnostics/resolveConstructor.kt");
	}

	@Test
	public void testaccessClassObjectFromJava() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/accessClassObjectFromJava.kt");
	}

	@Test
	public void testambiguousSamAdapters() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/ambiguousSamAdapters.kt");
	}

	@Test
	public void testcanDeclareIfSamAdapterIsInherited() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/canDeclareIfSamAdapterIsInherited.kt");
	}

	@Test
	public void testinheritAbstractSamAdapter() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/inheritAbstractSamAdapter.kt");
	}

	@Test
	public void testinnerNestedClassFromJava() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/innerNestedClassFromJava.kt");
	}

	@Test
	public void testinvisiblePackagePrivateInheritedMember() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/invisiblePackagePrivateInheritedMember.kt");
	}
	
	@Test
	@Ignore("Add when java-integration will be ready")
	public void testKJKInheritance() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/KJKInheritance.kt");
	}

	@Test
	@Ignore("Add when java-integration will be ready")
	public void testKJKInheritanceGeneric() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/KJKInheritanceGeneric.kt");
	}

	@Test
	public void testkt1402() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt1402.kt");
	}

	@Test
	public void testkt1431() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt1431.kt");
	}

	@Test
	public void testkt2152() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt2152.kt");
	}

	@Test
	public void testkt2394() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt2394.kt");
	}

	@Test
	public void testkt2606() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt2606.kt");
	}

	@Test
	public void testkt2619() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt2619.kt");
	}

	@Test
	public void testkt2641() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt2641.kt");
	}

	@Test
	public void testkt2890() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt2890.kt");
	}

	@Test
	public void testkt3307() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt3307.kt");
	}

	@Test
	public void testmutableIterator() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/mutableIterator.kt");
	}

	@Test
	public void testoverrideRawType() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/overrideRawType.kt");
	}

	@Test
	public void testOverrideVararg() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/OverrideVararg.kt");
	}

	@Test
	public void testpackageVisibility() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/packageVisibility.kt");
	}

	@Test
	public void testrecursiveRawUpperBound() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/recursiveRawUpperBound.kt");
	}

	@Test
	public void testSimple() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/Simple.kt");
	}

	@Test
	public void testStaticMembersFromSuperclasses() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/StaticMembersFromSuperclasses.kt");
	}

	@Test
	public void testSupertypeArgumentsNullabilityNotNullSpecialTypes() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/SupertypeArgumentsNullability-NotNull-SpecialTypes.kt");
	}

	@Test
	public void testSupertypeArgumentsNullabilityNotNullUserTypes() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/SupertypeArgumentsNullability-NotNull-UserTypes.kt");
	}

	@Test
	public void testSupertypeArgumentsNullabilitySpecialTypes() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/SupertypeArgumentsNullability-SpecialTypes.kt");
	}

	@Test
	public void testSupertypeArgumentsNullabilityUserTypes() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/SupertypeArgumentsNullability-UserTypes.kt");
	}

	@Test
	public void testGenericsInSupertypes() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/GenericsInSupertypes.kt");
	}
	
	@Test
	public void testInheritedGenericFunction() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/InheritedGenericFunction.kt");
	}
	
	@Test
	public void testInnerClassFromJava() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/InnerClassFromJava.kt");
	}
	
	@Test
	public void testkt1730_implementCharSequence() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt1730_implementCharSequence.kt");
	}
	
	@Test
	public void testkt3311() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/kt3311.kt");
	}
	
	@Test
	public void testUnboxingNulls() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/UnboxingNulls.kt");
	}
	
	@Test
	public void testpackagePrivateClassStaticMember() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/packagePrivateClassStaticMember.kt");
	}
	
	@Test
	public void testprivateNestedClassStaticMember() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/privateNestedClassStaticMember.kt");
	}
	
	@Test
	public void testprotectedStaticSamePackage() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/protectedStaticSamePackage.kt");
	}
	
	@Test
	public void testsamInConstructorWithGenerics() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/samInConstructorWithGenerics.kt");
	}
	
	@Test
	public void testtraitDefaultCall() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/j+k/traitDefaultCall.kt");
	}
}
