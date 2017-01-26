package org.jetbrains.kotlin.core.tests.diagnostics;

import org.jetbrains.kotlin.checkers.KotlinDiagnosticsTestCase;
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
    public void testAccessClassObjectFromJava() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/accessClassObjectFromJava.kt");
    }
    
    @Test
    public void testAmbiguousSamAdapters() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/ambiguousSamAdapters.kt");
    }
    
    @Test
    public void testAnnotationWithArgumentsMissingDependencies() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/annotationWithArgumentsMissingDependencies.kt");
    }
    
    @Test
    public void testArrayOfStarParametrized() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/arrayOfStarParametrized.kt");
    }
    
    @Test
    public void testCanDeclareIfSamAdapterIsInherited() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/canDeclareIfSamAdapterIsInherited.kt");
    }
    
    @Test
    public void testComputeIfAbsentConcurrent() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/computeIfAbsentConcurrent.kt");
    }
    
    @Test
    public void testContravariantIterable() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/contravariantIterable.kt");
    }
    
    @Test
    public void testEnumGetOrdinal() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/enumGetOrdinal.kt");
    }
    
    @Test
    public void testFieldOverridesField() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/fieldOverridesField.kt");
    }
    
    @Test
    public void testFieldOverridesFieldOfDifferentType() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/fieldOverridesFieldOfDifferentType.kt");
    }
    
    @Test
    public void testFinalCollectionSize() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/finalCollectionSize.kt");
    }
    
    @Test
    public void testGenericConstructorWithMultipleBounds() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/genericConstructorWithMultipleBounds.kt");
    }
    
    @Test
    public void testGenericsInSupertypes() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/GenericsInSupertypes.kt");
    }
    
    @Test
    public void testInheritAbstractSamAdapter() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/inheritAbstractSamAdapter.kt");
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
    public void testInnerNestedClassFromJava() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/innerNestedClassFromJava.kt");
    }
    
    @Test
    public void testInvisiblePackagePrivateInheritedMember() throws Exception {
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
    public void testJavaStaticImport() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/javaStaticImport.kt");
    }
    
    @Test
    public void testKt1402() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt1402.kt");
    }
    
    @Test
    public void testKt1431() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt1431.kt");
    }
    
    @Test
    public void testKt1730_implementCharSequence() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt1730_implementCharSequence.kt");
    }
    
    @Test
    public void testKt2152() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt2152.kt");
    }
    
    @Test
    public void testKt2394() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt2394.kt");
    }
    
    @Test
    public void testKt2606() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt2606.kt");
    }
    
    @Test
    public void testKt2619() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt2619.kt");
    }
    
    @Test
    public void testKt2641() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt2641.kt");
    }
    
    @Test
    public void testKt2890() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt2890.kt");
    }
    
    @Test
    public void testKt3307() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt3307.kt");
    }
    
    @Test
    public void testKt3311() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt3311.kt");
    }
    
    @Test
    public void testKt7523() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/kt7523.kt");
    }
    
    @Test
    public void testMatchers() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/matchers.kt");
    }
    
    @Test
    public void testMutableIterator() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/mutableIterator.kt");
    }
    
    @Test
    public void testOverrideRawType() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/overrideRawType.kt");
    }
    
    @Test
    public void testOverrideVararg() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/OverrideVararg.kt");
    }
    
    @Test
    public void testOverrideWithSamAndTypeParameter() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/overrideWithSamAndTypeParameter.kt");
    }
    
    @Test
    public void testPackagePrivateClassStaticMember() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/packagePrivateClassStaticMember.kt");
    }
    
    @Test
    public void testPackageVisibility() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/packageVisibility.kt");
    }
    
    @Test
    public void testPrivateNestedClassStaticMember() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/privateNestedClassStaticMember.kt");
    }
    
    @Test
    public void testProtectedStaticSamePackage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/protectedStaticSamePackage.kt");
    }
    
    @Test
    public void testRecursionWithJavaSyntheticProperty() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/recursionWithJavaSyntheticProperty.kt");
    }
    
    @Test
    public void testRecursiveRawUpperBound() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/recursiveRawUpperBound.kt");
    }
    
    @Test
    public void testRecursiveRawUpperBound2() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/recursiveRawUpperBound2.kt");
    }
    
    @Test
    public void testRecursiveRawUpperBound3() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/recursiveRawUpperBound3.kt");
    }
    
    @Test
    public void testSamInConstructorWithGenerics() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/samInConstructorWithGenerics.kt");
    }
    
    @Test
    public void testSerializable() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/serializable.kt");
    }
    
    @Test
    public void testShadowingPrimitiveStaticField() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/shadowingPrimitiveStaticField.kt");
    }
    
    @Test
    public void testSimple() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/Simple.kt");
    }
    
    @Test
    public void testSpecialBridges() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/specialBridges.kt");
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
    public void testTraitDefaultCall() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/traitDefaultCall.kt");
    }
    
    @Test
    public void testUnboxingNulls() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/j+k/UnboxingNulls.kt");
    }
}
