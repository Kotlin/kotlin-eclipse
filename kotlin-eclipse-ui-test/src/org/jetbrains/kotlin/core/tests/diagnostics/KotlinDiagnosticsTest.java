package org.jetbrains.kotlin.core.tests.diagnostics;

import org.junit.Test;

public class KotlinDiagnosticsTest extends KotlinDiagnosticsTestCase {
    
    @Test
    public void testAbstract() throws Exception {
        doTest("testData/diagnostics/Abstract.kt");
    }
    
    @Test
    public void testAbstractInAbstractClass() throws Exception {
        doTest("testData/diagnostics/AbstractInAbstractClass.kt");
    }
    
    @Test
    public void testAbstractInClass() throws Exception {
        doTest("testData/diagnostics/AbstractInClass.kt");
    }
    
    @Test
    public void testAbstractInTrait() throws Exception {
        doTest("testData/diagnostics/AbstractInTrait.kt");
    }
    
    @Test
    public void testAnonymousInitializers() throws Exception {
        doTest("testData/diagnostics/AnonymousInitializers.kt");
    }
    
    @Test
    public void testAnonymousInitializerVarAndConstructor() throws Exception {
        doTest("testData/diagnostics/AnonymousInitializerVarAndConstructor.kt");
    }
    
    @Test
    public void testAutocastAmbiguitites() throws Exception {
        doTest("testData/diagnostics/AutocastAmbiguitites.kt");
    }
    
    @Test
    public void testAutoCreatedIt() throws Exception {
        doTest("testData/diagnostics/AutoCreatedIt.kt");
    }
    
    @Test
    public void testBasic() throws Exception {
        doTest("testData/diagnostics/Basic.kt");
    }
    
    @Test
    public void testBinaryCallsOnNullableValues() throws Exception {
        doTest("testData/diagnostics/BinaryCallsOnNullableValues.kt");
    }
    
    @Test
    public void testBounds() throws Exception {
        doTest("testData/diagnostics/Bounds.kt");
    }
    
    @Test
    public void testBreakContinue() throws Exception {
        doTest("testData/diagnostics/BreakContinue.kt");
    }
    
    @Test
    public void testBuilders() throws Exception {
        doTest("testData/diagnostics/Builders.kt");
    }
    
    @Test
    public void testCasts() throws Exception {
        doTest("testData/diagnostics/Casts.kt");
    }
    
    @Test
    public void testCharacterLiterals() throws Exception {
        doTest("testData/diagnostics/CharacterLiterals.kt");
    }
    
    @Test
    public void testcheckType() throws Exception {
        doTest("testData/diagnostics/checkType.kt");
    }
    
    @Test
    public void testCompareToWithErrorType() throws Exception {
        doTest("testData/diagnostics/CompareToWithErrorType.kt");
    }
    
    @Test
    public void testConstants() throws Exception {
        doTest("testData/diagnostics/Constants.kt");
    }
    
    @Test
    public void testConstructors() throws Exception {
        doTest("testData/diagnostics/Constructors.kt");
    }
    
    @Test
    public void testConstructorsOfPrimitives() throws Exception {
        doTest("testData/diagnostics/ConstructorsOfPrimitives.kt");
    }
    
    @Test
    public void testCovariantOverrideType() throws Exception {
        doTest("testData/diagnostics/CovariantOverrideType.kt");
    }
    
    @Test
    public void testDefaultValuesTypechecking() throws Exception {
        doTest("testData/diagnostics/DefaultValuesTypechecking.kt");
    }
    
    @Test
    public void testDeferredTypes() throws Exception {
        doTest("testData/diagnostics/DeferredTypes.kt");
    }
    
    @Test
    public void testDelegationAndOverriding() throws Exception {
        doTest("testData/diagnostics/DelegationAndOverriding.kt");
    }
    
    @Test
    public void testDelegationNotTotrait() throws Exception {
        doTest("testData/diagnostics/DelegationNotTotrait.kt");
    }
    
    @Test
    public void testDelegationToJavaIface() throws Exception {
        doTest("testData/diagnostics/DelegationToJavaIface.kt");
    }
    
    @Test
    public void testDelegation_ClashingFunctions() throws Exception {
        doTest("testData/diagnostics/Delegation_ClashingFunctions.kt");
    }
    
    @Test
    public void testDelegation_Hierarchy() throws Exception {
        doTest("testData/diagnostics/Delegation_Hierarchy.kt");
    }
    
    @Test
    public void testDelegation_MultipleDelegates() throws Exception {
        doTest("testData/diagnostics/Delegation_MultipleDelegates.kt");
    }
    
    @Test
    public void testDelegation_ScopeInitializationOrder() throws Exception {
        doTest("testData/diagnostics/Delegation_ScopeInitializationOrder.kt");
    }
    
    @Test
    public void testDiamondFunction() throws Exception {
        doTest("testData/diagnostics/DiamondFunction.kt");
    }
    
    @Test
    public void testDiamondFunctionGeneric() throws Exception {
        doTest("testData/diagnostics/DiamondFunctionGeneric.kt");
    }
    
    @Test
    public void testDiamondProperty() throws Exception {
        doTest("testData/diagnostics/DiamondProperty.kt");
    }
    
    @Test
    public void testDollar() throws Exception {
        doTest("testData/diagnostics/Dollar.kt");
    }
    
    @Test
    public void testForRangeConventions() throws Exception {
        doTest("testData/diagnostics/ForRangeConventions.kt");
    }
    
    @Test
    public void testFunctionCalleeExpressions() throws Exception {
        doTest("testData/diagnostics/FunctionCalleeExpressions.kt");
    }
    
    @Test
    public void testFunctionReturnTypes() throws Exception {
        doTest("testData/diagnostics/FunctionReturnTypes.kt");
    }
    
    @Test
    public void testGenericArgumentConsistency() throws Exception {
        doTest("testData/diagnostics/GenericArgumentConsistency.kt");
    }
    
    @Test
    public void testGenericFunctionIsLessSpecific() throws Exception {
        doTest("testData/diagnostics/GenericFunctionIsLessSpecific.kt");
    }
    
    @Test
    public void testIllegalModifiers() throws Exception {
        doTest("testData/diagnostics/IllegalModifiers.kt");
    }
    
    @Test
    public void testIncDec() throws Exception {
        doTest("testData/diagnostics/IncDec.kt");
    }
    
    @Test
    public void testIncorrectCharacterLiterals() throws Exception {
        doTest("testData/diagnostics/IncorrectCharacterLiterals.kt");
    }
    
    @Test
    public void testInferNullabilityInThenBlock() throws Exception {
        doTest("testData/diagnostics/InferNullabilityInThenBlock.kt");
    }
    
    @Test
    public void testIsExpressions() throws Exception {
        doTest("testData/diagnostics/IsExpressions.kt");
    }
    
    @Test
    public void testkt310() throws Exception {
        doTest("testData/diagnostics/kt310.kt");
    }
    
    @Test
    public void testkt53() throws Exception {
        doTest("testData/diagnostics/kt53.kt");
    }
    
    @Test
    public void testLValueAssignment() throws Exception {
        doTest("testData/diagnostics/LValueAssignment.kt");
    }
    
    @Test
    public void testMergePackagesWithJava() throws Exception {
        doTest("testData/diagnostics/MergePackagesWithJava.kt");
    }
    
    @Test
    public void testMultilineStringTemplates() throws Exception {
        doTest("testData/diagnostics/MultilineStringTemplates.kt");
    }
    
    @Test
    public void testMultipleBounds() throws Exception {
        doTest("testData/diagnostics/MultipleBounds.kt");
    }
    
    @Test
    public void testNullability() throws Exception {
        doTest("testData/diagnostics/Nullability.kt");
    }
    
    @Test
    public void testOverrideFunctionWithParamDefaultValue() throws Exception {
        doTest("testData/diagnostics/OverrideFunctionWithParamDefaultValue.kt");
    }
    
    @Test
    public void testOverridenFunctionAndSpecifiedTypeParameter() throws Exception {
        doTest("testData/diagnostics/OverridenFunctionAndSpecifiedTypeParameter.kt");
    }
    
    @Test
    public void testOverridingVarByVal() throws Exception {
        doTest("testData/diagnostics/OverridingVarByVal.kt");
    }
    
    @Test
    public void testPackageAsExpression() throws Exception {
        doTest("testData/diagnostics/PackageAsExpression.kt");
    }
    
    @Test
    public void testPackageInExpressionPosition() throws Exception {
        doTest("testData/diagnostics/PackageInExpressionPosition.kt");
    }
    
    @Test
    public void testPackageQualified() throws Exception {
        doTest("testData/diagnostics/PackageQualified.kt");
    }
    
    @Test
    public void testPrimaryConstructors() throws Exception {
        doTest("testData/diagnostics/PrimaryConstructors.kt");
    }
    
    @Test
    public void testProcessingEmptyImport() throws Exception {
        doTest("testData/diagnostics/ProcessingEmptyImport.kt");
    }
    
    @Test
    public void testProjectionOnFunctionArgumentErrror() throws Exception {
        doTest("testData/diagnostics/ProjectionOnFunctionArgumentErrror.kt");
    }
    
    @Test
    public void testProjectionsInSupertypes() throws Exception {
        doTest("testData/diagnostics/ProjectionsInSupertypes.kt");
    }
    
    @Test
    public void testProperties() throws Exception {
        doTest("testData/diagnostics/Properties.kt");
    }
    
    @Test
    public void testPropertyInitializers() throws Exception {
        doTest("testData/diagnostics/PropertyInitializers.kt");
    }
    
    @Test
    public void testQualifiedExpressions() throws Exception {
        doTest("testData/diagnostics/QualifiedExpressions.kt");
    }
    
    @Test
    public void testRecursiveTypeInference() throws Exception {
        doTest("testData/diagnostics/RecursiveTypeInference.kt");
    }
    
    @Test
    public void testResolveOfJavaGenerics() throws Exception {
        doTest("testData/diagnostics/ResolveOfJavaGenerics.kt");
    }
    
    @Test
    public void testResolveToJava() throws Exception {
        doTest("testData/diagnostics/ResolveToJava.kt");
    }
    
    @Test
    public void testReturn() throws Exception {
        doTest("testData/diagnostics/Return.kt");
    }
    
    @Test
    public void testSafeCallNonNullReceiver() throws Exception {
        doTest("testData/diagnostics/SafeCallNonNullReceiver.kt");
    }
    
    @Test
    public void testSafeCallNonNullReceiverReturnNull() throws Exception {
        doTest("testData/diagnostics/SafeCallNonNullReceiverReturnNull.kt");
    }
    
    @Test
    public void testSafeCallOnFakePackage() throws Exception {
        doTest("testData/diagnostics/SafeCallOnFakePackage.kt");
    }
    
    @Test
    public void testShiftFunctionTypes() throws Exception {
        doTest("testData/diagnostics/ShiftFunctionTypes.kt");
    }
    
    @Test
    public void testStarsInFunctionCalls() throws Exception {
        doTest("testData/diagnostics/StarsInFunctionCalls.kt");
    }
    
    @Test
    public void testStringTemplates() throws Exception {
        doTest("testData/diagnostics/StringTemplates.kt");
    }
    
    @Test
    public void testSupertypeListChecks() throws Exception {
        doTest("testData/diagnostics/SupertypeListChecks.kt");
    }
    
    @Test
    public void testSyntaxErrorInTestHighlighting() throws Exception {
        doTest("testData/diagnostics/SyntaxErrorInTestHighlighting.kt");
    }
    
    @Test
    public void testSyntaxErrorInTestHighlightingEof() throws Exception {
        doTest("testData/diagnostics/SyntaxErrorInTestHighlightingEof.kt");
    }
    
    @Test
    public void testTraitOverrideObjectMethods() throws Exception {
        doTest("testData/diagnostics/TraitOverrideObjectMethods.kt");
    }
    
    @Test
    public void testTraitSupertypeList() throws Exception {
        doTest("testData/diagnostics/TraitSupertypeList.kt");
    }
    
    @Test
    public void testTypeInference() throws Exception {
        doTest("testData/diagnostics/TypeInference.kt");
    }
    
    @Test
    public void testTypeMismatchOnOverrideWithSyntaxErrors() throws Exception {
        doTest("testData/diagnostics/TypeMismatchOnOverrideWithSyntaxErrors.kt");
    }
    
    @Test
    public void testUnitByDefaultForFunctionTypes() throws Exception {
        doTest("testData/diagnostics/UnitByDefaultForFunctionTypes.kt");
    }
    
    @Test
    public void testUnitValue() throws Exception {
        doTest("testData/diagnostics/UnitValue.kt");
    }
    
    @Test
    public void testUnresolved() throws Exception {
        doTest("testData/diagnostics/Unresolved.kt");
    }
    
    @Test
    public void testUnusedVariables() throws Exception {
        doTest("testData/diagnostics/UnusedVariables.kt");
    }
    
    @Test
    public void testValAndFunOverrideCompatibilityClash() throws Exception {
        doTest("testData/diagnostics/ValAndFunOverrideCompatibilityClash.kt");
    }
    
    @Test
    public void testVarargs() throws Exception {
        doTest("testData/diagnostics/Varargs.kt");
    }
    
    @Test
    public void testVarargTypes() throws Exception {
        doTest("testData/diagnostics/VarargTypes.kt");
    }
    
    @Test
    public void testVariance() throws Exception {
        doTest("testData/diagnostics/Variance.kt");
    }
    
    @Test
    public void testImplementInterfaceAndOverrideMethodFromAny() throws Exception {
        doTest("testData/diagnostics/ImplementInterfaceAndOverrideMethodFromAny.kt");
    }
    
    @Test
    public void testabstractClass() throws Exception {
    	doTest("testData/diagnostics/abstractClass.kt");
    }
    
    @Test
    public void testanonymousObjectExtendsTraitWithRequired() throws Exception {
    	doTest("testData/diagnostics/anonymousObjectExtendsTraitWithRequired.kt");
    }
    
    @Test
    public void testdefaultModifier() throws Exception {
    	doTest("testData/diagnostics/defaultModifier.kt");
    }
    
    @Test
    public void testdifferentGenericArguments() throws Exception {
    	doTest("testData/diagnostics/differentGenericArguments.kt");
    }
    
    @Test
    public void testincompatibleVarianceModifiers() throws Exception {
    	doTest("testData/diagnostics/incompatibleVarianceModifiers.kt");
    }
    
    @Test
    public void testkt3006() throws Exception {
    	doTest("testData/diagnostics/kt3006.kt");
    }
    
    @Test
    public void testmanyRequirementsDisallowed() throws Exception {
    	doTest("testData/diagnostics/manyRequirementsDisallowed.kt");
    }
    
    @Test
    public void testmanyTraitsRequireSameClass() throws Exception {
    	doTest("testData/diagnostics/manyTraitsRequireSameClass.kt");
    }
    
    @Test
    public void testprimaryConstructorMissingKeyword() throws Exception {
    	doTest("testData/diagnostics/primaryConstructorMissingKeyword.kt");
    }
    
    @Test
    public void testrepeatedModifiers() throws Exception {
    	doTest("testData/diagnostics/repeatedModifiers.kt");
    }
    
    @Test
    public void testrequirementFulfilledBySubclass() throws Exception {
    	doTest("testData/diagnostics/requirementFulfilledBySubclass.kt");
    }
    
    @Test
    public void testtraitExtendsTraitWithRequired() throws Exception {
    	doTest("testData/diagnostics/traitExtendsTraitWithRequired.kt");
    }
    
    @Test
    public void testtraitRequiresAny() throws Exception {
    	doTest("testData/diagnostics/traitRequiresAny.kt");
    }
    
    @Test
    public void testuninheritableTraitDifferentGenericArguments() throws Exception {
    	doTest("testData/diagnostics/uninheritableTraitDifferentGenericArguments.kt");
    }
    
    @Test
	public void testUnusedParameters() throws Exception {
		doTest("testData/diagnostics/UnusedParameters.kt");
	}
    
	@Test
	public void testTraitWithConstructor() throws Exception {
		doTest("testData/diagnostics/TraitWithConstructor.kt");
	}
	
	@Test
	public void testSelfTypesUnsupported() throws Exception {
		doTest("testData/diagnostics/SelfTypesUnsupported.kt");
	}
	
	@Test
	public void testPackageInTypePosition() throws Exception {
		doTest("testData/diagnostics/PackageInTypePosition.kt");
	}
	
	@Test
	public void testRecursiveResolve() throws Exception {
		doTest("testData/diagnostics/RecursiveResolve.kt");
	}
	
	@Test
	public void testLocalClassAndShortSubpackageNames() throws Exception {
		doTest("testData/diagnostics/LocalClassAndShortSubpackageNames.kt");
	}
	
	@Test
	public void testFunctionParameterWithoutType() throws Exception {
		doTest("testData/diagnostics/FunctionParameterWithoutType.kt");
	}
	
	@Test
	public void testDeprecatedTraitKeyword() throws Exception {
		doTest("testData/diagnostics/DeprecatedTraitKeyword.kt");
	}
	
	@Test
	public void testDelegationExpectedType() throws Exception {
		doTest("testData/diagnostics/DelegationExpectedType.kt");
	}
	
	@Test
	public void testBreakContinueInWhen() throws Exception {
		doTest("testData/diagnostics/BreakContinueInWhen.kt");
	}
	
	@Test
	public void testAssignToArrayElement() throws Exception {
		doTest("testData/diagnostics/AssignToArrayElement.kt");
	}
}
