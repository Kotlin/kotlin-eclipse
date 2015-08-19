package org.jetbrains.kotlin.core.tests.diagnostics;

import org.junit.Test;

public class KotlinDiagnosticsTest extends KotlinDiagnosticsTestCase {
    
    @Test
    public void testAbstract() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Abstract.kt");
    }
    
    @Test
    public void testAbstractInAbstractClass() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/AbstractInAbstractClass.kt");
    }
    
    @Test
    public void testAbstractInClass() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/AbstractInClass.kt");
    }
    
    @Test
    public void testAbstractInTrait() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/AbstractInTrait.kt");
    }
    
    @Test
    public void testAnonymousInitializers() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/AnonymousInitializers.kt");
    }
    
    @Test
    public void testAnonymousInitializerVarAndConstructor() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/AnonymousInitializerVarAndConstructor.kt");
    }
    
    @Test
    public void testAutoCreatedIt() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/AutoCreatedIt.kt");
    }
    
    @Test
    public void testBasic() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Basic.kt");
    }
    
    @Test
    public void testBinaryCallsOnNullableValues() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/BinaryCallsOnNullableValues.kt");
    }
    
    @Test
    public void testBounds() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Bounds.kt");
    }
    
    @Test
    public void testBreakContinue() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/BreakContinue.kt");
    }
    
    @Test
    public void testBuilders() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Builders.kt");
    }
    
    @Test
    public void testCasts() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Casts.kt");
    }
    
    @Test
    public void testCharacterLiterals() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/CharacterLiterals.kt");
    }
    
    @Test
    public void testcheckType() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/checkType.kt");
    }
    
    @Test
    public void testCompareToWithErrorType() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/CompareToWithErrorType.kt");
    }
    
    @Test
    public void testConstants() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Constants.kt");
    }
    
    @Test
    public void testConstructors() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Constructors.kt");
    }
    
    @Test
    public void testConstructorsOfPrimitives() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ConstructorsOfPrimitives.kt");
    }
    
    @Test
    public void testCovariantOverrideType() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/CovariantOverrideType.kt");
    }
    
    @Test
    public void testDefaultValuesTypechecking() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/DefaultValuesTypechecking.kt");
    }
    
    @Test
    public void testDeferredTypes() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/DeferredTypes.kt");
    }
    
    @Test
    public void testDelegationAndOverriding() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/delegation/DelegationAndOverriding.kt");
    }
    
    @Test
    public void testDelegationNotTotrait() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/delegation/DelegationNotTotrait.kt");
    }
    
    @Test
    public void testDelegationToJavaIface() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/delegation/DelegationToJavaIface.kt");
    }
    
    @Test
    public void testDelegation_ClashingFunctions() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/delegation/Delegation_ClashingFunctions.kt");
    }
    
    @Test
    public void testDelegation_Hierarchy() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/delegation/Delegation_Hierarchy.kt");
    }
    
    @Test
    public void testDelegation_MultipleDelegates() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/delegation/Delegation_MultipleDelegates.kt");
    }
    
    @Test
    public void testDelegation_ScopeInitializationOrder() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/delegation/Delegation_ScopeInitializationOrder.kt");
    }
    
    @Test
    public void testDiamondFunction() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/DiamondFunction.kt");
    }
    
    @Test
    public void testDiamondFunctionGeneric() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/DiamondFunctionGeneric.kt");
    }
    
    @Test
    public void testDiamondProperty() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/DiamondProperty.kt");
    }
    
    @Test
    public void testDollar() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Dollar.kt");
    }
    
    @Test
    public void testForRangeConventions() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ForRangeConventions.kt");
    }
    
    @Test
    public void testFunctionCalleeExpressions() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/FunctionCalleeExpressions.kt");
    }
    
    @Test
    public void testFunctionReturnTypes() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/FunctionReturnTypes.kt");
    }
    
    @Test
    public void testGenericArgumentConsistency() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/GenericArgumentConsistency.kt");
    }
    
    @Test
    public void testGenericFunctionIsLessSpecific() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/GenericFunctionIsLessSpecific.kt");
    }
    
    @Test
    public void testIllegalModifiers() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/modifiers/IllegalModifiers.kt");
    }
    
    @Test
    public void testIncDec() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/IncDec.kt");
    }
    
    @Test
    public void testIncorrectCharacterLiterals() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/IncorrectCharacterLiterals.kt");
    }
    
    @Test
    public void testInferNullabilityInThenBlock() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/InferNullabilityInThenBlock.kt");
    }
    
    @Test
    public void testIsExpressions() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/IsExpressions.kt");
    }
    
    @Test
    public void testkt310() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/kt310.kt");
    }
    
    @Test
    public void testkt53() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/kt53.kt");
    }
    
    @Test
    public void testLValueAssignment() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/LValueAssignment.kt");
    }
    
    @Test
    public void testMultilineStringTemplates() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/MultilineStringTemplates.kt");
    }
    
    @Test
    public void testMultipleBounds() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/MultipleBounds.kt");
    }
    
    @Test
    public void testNullability() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Nullability.kt");
    }
    
    @Test
    public void testOverrideFunctionWithParamDefaultValue() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/OverrideFunctionWithParamDefaultValue.kt");
    }
    
    @Test
    public void testOverridenFunctionAndSpecifiedTypeParameter() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/OverridenFunctionAndSpecifiedTypeParameter.kt");
    }
    
    @Test
    public void testOverridingVarByVal() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/OverridingVarByVal.kt");
    }
    
    @Test
    public void testPackageAsExpression() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/PackageAsExpression.kt");
    }
    
    @Test
    public void testPackageInExpressionPosition() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/PackageInExpressionPosition.kt");
    }
    
    @Test
    public void testPackageQualified() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/PackageQualified.kt");
    }
    
    @Test
    public void testPrimaryConstructors() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/PrimaryConstructors.kt");
    }
    
    @Test
    public void testProcessingEmptyImport() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ProcessingEmptyImport.kt");
    }
    
    @Test
    public void testProjectionOnFunctionArgumentErrror() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ProjectionOnFunctionArgumentErrror.kt");
    }
    
    @Test
    public void testProjectionsInSupertypes() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ProjectionsInSupertypes.kt");
    }
    
    @Test
    public void testProperties() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Properties.kt");
    }
    
    @Test
    public void testPropertyInitializers() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/PropertyInitializers.kt");
    }
    
    @Test
    public void testQualifiedExpressions() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/QualifiedExpressions.kt");
    }
    
    @Test
    public void testRecursiveTypeInference() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/RecursiveTypeInference.kt");
    }
    
    @Test
    public void testResolveOfJavaGenerics() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ResolveOfJavaGenerics.kt");
    }
    
    @Test
    public void testResolveToJava() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ResolveToJava.kt");
    }
    
    @Test
    public void testReturn() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Return.kt");
    }
    
    @Test
    public void testSafeCallNonNullReceiver() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/SafeCallNonNullReceiver.kt");
    }
    
    @Test
    public void testSafeCallNonNullReceiverReturnNull() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/SafeCallNonNullReceiverReturnNull.kt");
    }
    
    @Test
    public void testSafeCallOnFakePackage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/SafeCallOnFakePackage.kt");
    }
    
    @Test
    public void testShiftFunctionTypes() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ShiftFunctionTypes.kt");
    }
    
    @Test
    public void testStarsInFunctionCalls() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/StarsInFunctionCalls.kt");
    }
    
    @Test
    public void testStringTemplates() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/StringTemplates.kt");
    }
    
    @Test
    public void testSupertypeListChecks() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/SupertypeListChecks.kt");
    }
    
    @Test
    public void testSyntaxErrorInTestHighlighting() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/SyntaxErrorInTestHighlighting.kt");
    }
    
    @Test
    public void testSyntaxErrorInTestHighlightingEof() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/SyntaxErrorInTestHighlightingEof.kt");
    }
    
    @Test
    public void testTraitOverrideObjectMethods() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/TraitOverrideObjectMethods.kt");
    }
    
    @Test
    public void testTraitSupertypeList() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/traitWithRequired/traitSupertypeList.kt");
    }
    
    @Test
    public void testTypeInference() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/TypeInference.kt");
    }
    
    @Test
    public void testTypeMismatchOnOverrideWithSyntaxErrors() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/TypeMismatchOnOverrideWithSyntaxErrors.kt");
    }
    
    @Test
    public void testUnitByDefaultForFunctionTypes() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/UnitByDefaultForFunctionTypes.kt");
    }
    
    @Test
    public void testUnitValue() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/UnitValue.kt");
    }
    
    @Test
    public void testUnresolved() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Unresolved.kt");
    }
    
    @Test
    public void testUnusedVariables() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/UnusedVariables.kt");
    }
    
    @Test
    public void testValAndFunOverrideCompatibilityClash() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/ValAndFunOverrideCompatibilityClash.kt");
    }
    
    @Test
    public void testVarargs() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Varargs.kt");
    }
    
    @Test
    public void testVarargTypes() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/VarargTypes.kt");
    }
    
    @Test
    public void testVariance() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/Variance.kt");
    }
    
    @Test
    public void testdefaultModifier() throws Exception {
    	doTest("common_testData/compiler/diagnostics/tests/modifiers/defaultModifier.kt");
    }
    
    @Test
    public void testincompatibleVarianceModifiers() throws Exception {
    	doTest("common_testData/compiler/diagnostics/tests/modifiers/incompatibleVarianceModifiers.kt");
    }
    
    @Test
    public void testprimaryConstructorMissingKeyword() throws Exception {
    	doTest("common_testData/compiler/diagnostics/tests/modifiers/primaryConstructorMissingKeyword.kt");
    }
    
    @Test
    public void testrepeatedModifiers() throws Exception {
    	doTest("common_testData/compiler/diagnostics/tests/modifiers/repeatedModifiers.kt");
    }
    
    @Test
    public void testtraitRequiresAny() throws Exception {
    	doTest("common_testData/compiler/diagnostics/tests/traitWithRequired/traitRequiresAny.kt");
    }
    
    @Test
	public void testUnusedParameters() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/UnusedParameters.kt");
	}
    
	@Test
	public void testTraitWithConstructor() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/TraitWithConstructor.kt");
	}
	
	@Test
	public void testSelfTypesUnsupported() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/SelfTypesUnsupported.kt");
	}
	
	@Test
	public void testPackageInTypePosition() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/PackageInTypePosition.kt");
	}
	
	@Test
	public void testRecursiveResolve() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/RecursiveResolve.kt");
	}
	
	@Test
	public void testLocalClassAndShortSubpackageNames() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/LocalClassAndShortSubpackageNames.kt");
	}
	
	@Test
	public void testFunctionParameterWithoutType() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/FunctionParameterWithoutType.kt");
	}
	
	@Test
	public void testDeprecatedTraitKeyword() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/DeprecatedTraitKeyword.kt");
	}
	
	@Test
	public void testDelegationExpectedType() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/delegation/DelegationExpectedType.kt");
	}
	
	@Test
	public void testBreakContinueInWhen() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/BreakContinueInWhen.kt");
	}
	
	@Test
	public void testAssignToArrayElement() throws Exception {
		doTest("common_testData/compiler/diagnostics/tests/AssignToArrayElement.kt");
	}
	
	//deprecation tests
	@Test
    public void testAnnotationUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/annotationUsage.kt");
    }

	@Test
    public void testComponentUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/componentUsage.kt");
    }
	
	@Test
    public void testFunctionUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/functionUsage.kt");
    }

	@Test
    public void testGenericConstructorUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/genericConstructorUsage.kt");
    }

	@Test
    public void testImports() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/imports.kt");
    }

	@Test
    public void testIteratorUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/iteratorUsage.kt");
    }

	@Test
    public void testJavaDeprecated() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/javaDeprecated.kt");
    }

	@Test
    public void testJavaDocDeprecated() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/javaDocDeprecated.kt");
    }

	@Test
    public void testNestedTypesUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/nestedTypesUsage.kt");
    }

	@Test
    public void testObjectUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/objectUsage.kt");
    }

	@Test
    public void testPropertyUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/propertyUsage.kt");
    }

	@Test
    public void testTypeUsage() throws Exception {
        doTest("common_testData/compiler/diagnostics/tests/deprecated/typeUsage.kt");
    }
}