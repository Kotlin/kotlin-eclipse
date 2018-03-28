package org.jetbrains.kotlin.ui.tests.editors.completion;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinKeywordCompletionTest extends KotlinKeywordCompletionTestCase {
    
    @Ignore
    @Test
    public void testAfterClasses() {
        doTest("common_testData/ide/completion/keywords/AfterClasses.kt");
    }
    
    @Test
    public void testAfterDot() {
        doTest("common_testData/ide/completion/keywords/AfterDot.kt");
    }
    
    @Ignore
    @Test
    public void testAfterFuns() {
        doTest("common_testData/ide/completion/keywords/AfterFuns.kt");
    }
    
    @Test
    public void testAfterSafeDot() {
        doTest("common_testData/ide/completion/keywords/AfterSafeDot.kt");
    }
    
    @Test
    public void testAfterSpaceAndDot() {
        doTest("common_testData/ide/completion/keywords/AfterSpaceAndDot.kt");
    }
    
    @Test
    public void testBreakContinue() {
        doTest("common_testData/ide/completion/keywords/BreakContinue.kt");
    }
    
    @Test
    public void testBreakWithLabel() {
        doTest("common_testData/ide/completion/keywords/BreakWithLabel.kt");
    }
    
    @Test
    public void testCommaExpected() {
        doTest("common_testData/ide/completion/keywords/CommaExpected.kt");
    }
    
    @Test
    public void testCompanionObjectBeforeObject() {
        doTest("common_testData/ide/completion/keywords/CompanionObjectBeforeObject.kt");
    }
    
    @Test
    public void testContinueWithLabel() {
        doTest("common_testData/ide/completion/keywords/ContinueWithLabel.kt");
    }
    
    @Ignore
    @Test
    public void testInAnnotationClassScope() {
        doTest("common_testData/ide/completion/keywords/InAnnotationClassScope.kt");
    }
    
    @Test
    public void testInBlockComment() {
        doTest("common_testData/ide/completion/keywords/InBlockComment.kt");
    }
    
    @Test
    public void testInChar() {
        doTest("common_testData/ide/completion/keywords/InChar.kt");
    }
    
    @Ignore
    @Test
    public void testInClassBeforeFun() {
        doTest("common_testData/ide/completion/keywords/InClassBeforeFun.kt");
    }
    
    @Test
    public void testInClassNoCompletionInValName() {
        doTest("common_testData/ide/completion/keywords/InClassNoCompletionInValName.kt");
    }
    
    @Test
    public void testInClassProperty() {
        doTest("common_testData/ide/completion/keywords/InClassProperty.kt");
    }
    
    @Ignore
    @Test
    public void testInClassScope() {
        doTest("common_testData/ide/completion/keywords/InClassScope.kt");
    }
    
    @Test
    public void testInClassTypeParameters() {
        doTest("common_testData/ide/completion/keywords/InClassTypeParameters.kt");
    }
    
    @Test
    public void testInEnumScope1() {
        doTest("common_testData/ide/completion/keywords/InEnumScope1.kt");
    }
    
    @Ignore
    @Test
    public void testInEnumScope2() {
        doTest("common_testData/ide/completion/keywords/InEnumScope2.kt");
    }
    
    @Test
    public void testInFunctionExpressionBody() {
        doTest("common_testData/ide/completion/keywords/InFunctionExpressionBody.kt");
    }
    
    @Test
    public void testInFunctionName() {
        doTest("common_testData/ide/completion/keywords/InFunctionName.kt");
    }
    
    @Test
    public void testInFunctionRecieverType() {
        doTest("common_testData/ide/completion/keywords/InFunctionRecieverType.kt");
    }
    
    @Test
    public void testInFunctionTypePosition() {
        doTest("common_testData/ide/completion/keywords/InFunctionTypePosition.kt");
    }
    
    @Test
    public void testInGetterExpressionBody() {
        doTest("common_testData/ide/completion/keywords/InGetterExpressionBody.kt");
    }
    
    @Ignore
    @Test
    public void testInInterfaceScope() {
        doTest("common_testData/ide/completion/keywords/InInterfaceScope.kt");
    }
    
    @Test
    public void testInMemberFunParametersList() {
        doTest("common_testData/ide/completion/keywords/InMemberFunParametersList.kt");
    }
    
    @Test
    public void testInModifierListInsideClass() {
        doTest("common_testData/ide/completion/keywords/InModifierListInsideClass.kt");
    }
    
    @Test
    public void testInNotFinishedGenericWithFunAfter() {
        doTest("common_testData/ide/completion/keywords/InNotFinishedGenericWithFunAfter.kt");
    }
    
    @Ignore
    @Test
    public void testInObjectScope() {
        doTest("common_testData/ide/completion/keywords/InObjectScope.kt");
    }
    
    @Ignore
    @Test
    public void testInPrimaryConstructorParametersList() {
        doTest("common_testData/ide/completion/keywords/InPrimaryConstructorParametersList.kt");
    }
    
    @Test
    public void testInPropertyInitializer() {
        doTest("common_testData/ide/completion/keywords/InPropertyInitializer.kt");
    }
    
    @Test
    public void testInPropertyTypeReference() {
        doTest("common_testData/ide/completion/keywords/InPropertyTypeReference.kt");
    }
    
    @Test
    public void testInString() {
        doTest("common_testData/ide/completion/keywords/InString.kt");
    }
    
    @Test
    public void testInTopFunParametersList() {
        doTest("common_testData/ide/completion/keywords/InTopFunParametersList.kt");
    }
    
    @Ignore
    @Test
    public void testInTopScopeAfterPackage() {
        doTest("common_testData/ide/completion/keywords/InTopScopeAfterPackage.kt");
    }
    
    @Test
    public void testLabeledLambdaThis() {
        doTest("common_testData/ide/completion/keywords/LabeledLambdaThis.kt");
    }
    
    @Test
    public void testLineComment() {
        doTest("common_testData/ide/completion/keywords/LineComment.kt");
    }
    
    @Test
    public void testNoBreak1() {
        doTest("common_testData/ide/completion/keywords/NoBreak1.kt");
    }
    
    @Test
    public void testNoBreak2() {
        doTest("common_testData/ide/completion/keywords/NoBreak2.kt");
    }
    
    @Test
    public void testNoCompletionForCapitalPrefix() {
        doTest("common_testData/ide/completion/keywords/NoCompletionForCapitalPrefix.kt");
    }
    
    @Test
    public void testNoContinue() {
        doTest("common_testData/ide/completion/keywords/NoContinue.kt");
    }
    
    @Test
    public void testNoFinalInParameterList() {
        doTest("common_testData/ide/completion/keywords/NoFinalInParameterList.kt");
    }
    
    @Test
    public void testNotInNotIs() {
        doTest("common_testData/ide/completion/keywords/NotInNotIs.kt");
    }
    
    @Test
    public void testNotInNotIs2() {
        doTest("common_testData/ide/completion/keywords/NotInNotIs2.kt");
    }
    
    @Test
    public void testPrefixMatcher() {
        doTest("common_testData/ide/completion/keywords/PrefixMatcher.kt");
    }
    
    @Test
    public void testQualifiedThis() {
        doTest("common_testData/ide/completion/keywords/QualifiedThis.kt");
    }
    
    @Test
    public void testReturn1() {
        doTest("common_testData/ide/completion/keywords/Return1.kt");
    }
    
    @Test
    public void testReturn2() {
        doTest("common_testData/ide/completion/keywords/Return2.kt");
    }
    
    @Test
    public void testReturn3() {
        doTest("common_testData/ide/completion/keywords/Return3.kt");
    }
    
    @Test
    public void testReturn4() {
        doTest("common_testData/ide/completion/keywords/Return4.kt");
    }
    
    @Test
    public void testReturn5() {
        doTest("common_testData/ide/completion/keywords/Return5.kt");
    }
    
    @Test
    public void testReturn6() {
        doTest("common_testData/ide/completion/keywords/Return6.kt");
    }
    
    @Test
    public void testReturn8() {
        doTest("common_testData/ide/completion/keywords/Return8.kt");
    }
    
    @Test
    public void testReturn9() {
        doTest("common_testData/ide/completion/keywords/Return9.kt");
    }
    
    @Test
    public void testReturnBoolean() {
        doTest("common_testData/ide/completion/keywords/ReturnBoolean.kt");
    }
    
    @Test
    public void testReturnCollection() {
        doTest("common_testData/ide/completion/keywords/ReturnCollection.kt");
    }
    
    @Test
    public void testReturnIterable() {
        doTest("common_testData/ide/completion/keywords/ReturnIterable.kt");
    }
    
    @Test
    public void testReturnKeywordName() {
        doTest("common_testData/ide/completion/keywords/ReturnKeywordName.kt");
    }
    
    @Test
    public void testReturnList() {
        doTest("common_testData/ide/completion/keywords/ReturnList.kt");
    }
    
    @Test
    public void testReturnNotNull() {
        doTest("common_testData/ide/completion/keywords/ReturnNotNull.kt");
    }
    
    @Test
    public void testReturnNull() {
        doTest("common_testData/ide/completion/keywords/ReturnNull.kt");
    }
    
    @Test
    public void testReturnNullableBoolean() {
        doTest("common_testData/ide/completion/keywords/ReturnNullableBoolean.kt");
    }
    
    @Test
    public void testReturnSet() {
        doTest("common_testData/ide/completion/keywords/ReturnSet.kt");
    }
    
    @Test
    public void testThis() {
        doTest("common_testData/ide/completion/keywords/This.kt");
    }
    
    @Test
    public void testThisPrefixMatching() {
        doTest("common_testData/ide/completion/keywords/ThisPrefixMatching.kt");
    }
    
    @Ignore
    @Test
    public void testTopScope() {
        doTest("common_testData/ide/completion/keywords/TopScope.kt");
    }
    
    @Test
    public void testUseSiteTargetForPrimaryConstructorParameter() {
        doTest("common_testData/ide/completion/keywords/UseSiteTargetForPrimaryConstructorParameter.kt");
    }
}
