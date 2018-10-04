package org.jetbrains.kotlin.ui.tests.editors.formatter;

import org.junit.Test;

public class KotlinIdeaFormatActionTest extends KotlinFormatActionTestCase {
    @Override
    protected AfterSuffixPosition getAfterPosition() {
        return AfterSuffixPosition.BEFORE_DOT;
    }
    
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/formatter/";
    }
    
    @Test
    public void AfterSemiColonInEnumClass() {
        doAutoTest();
    }
    
    @Test
    public void AnonymousInitializers() {
        doAutoTest();
    }
    
    @Test
    public void AnonymousInitializersLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void ArrayAccess() {
        doAutoTest();
    }
    
    @Test
    public void BinaryExpressionAlignmentSpread() {
        doAutoTest();
    }
    
    @Test
    public void BinaryExpressions() {
        doAutoTest();
    }
    
    @Test
    public void BinaryExpressionsBoolean() {
        doAutoTest();
    }
    
    @Test
    public void BinaryExpressionsWithoutAlignment() {
        doAutoTest();
    }
    
    @Test
    public void BlockFor() {
        doAutoTest();
    }
    
    @Test
    public void CatchFinallyOnNewLine() {
        doAutoTest();
    }
    
    @Test
    public void Class() {
        doAutoTest();
    }
    
    @Test
    public void ClassInBody() {
        doAutoTest();
    }
    
    @Test
    public void ClassLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void ColonSpaces() {
        doAutoTest();
    }
    
    @Test
    public void CommentInFunctionLiteral() {
        doAutoTest();
    }
    
    @Test
    public void DelegationList() {
        doAutoTest();
    }
    
    @Test
    public void DocComments() {
        doAutoTest();
    }
    
    @Test
    public void DoWhileLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void DoWhileSpacing() {
        doAutoTest();
    }
    
    @Test
    public void ElseOnNewLine() {
        doAutoTest();
    }
    
    @Test
    public void Elvis() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineAfterObjectDeclaration() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineAfterPackage() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineBetweeAbstractFunctions() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineBetweenClassAndFunction() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineBetweenClasses() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineBetweenEnumEntries() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineBetweenFunAndProperty() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineBetweenFunctions() {
        doAutoTest();
    }
    
    @Test
    public void EmptyLineBetweenProperties() {
        doAutoTest();
    }
    
    @Test
    public void ForLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void FormatFirstColumnComments() {
        doAutoTest();
    }
    
    @Test
    public void FormatFirstColumnCommentsBeforeDeclaration() {
        doAutoTest();
    }
    
    @Test
    public void ForNoBraces() {
        doAutoTest();
    }
    
    @Test
    public void ForSpacing() {
        doAutoTest();
    }
    
    @Test
    public void FunctionalType() {
        doAutoTest();
    }
    
    @Test
    public void FunctionCallParametersAlign() {
        doAutoTest();
    }
    
    @Test
    public void FunctionDefParametersAlign() {
        doAutoTest();
    }
    
    @Test
    public void FunctionExpression() {
        doAutoTest();
    }
    
    @Test
    public void FunctionLineBreak() {
        doAutoTest();
    }

    @Test
    public void FunctionWithInference() {
        doAutoTest();
    }
    
    @Test
    public void GetterAndSetter() {
        doAutoTest();
    }
    
    @Test
    public void If() {
        doAutoTest();
    }
    
    @Test
    public void IfElseRemoveLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void IfElseWithLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void IfElseWithTrickyComments() {
        doAutoTest();
    }
    
    @Test
    public void IfSpacing() {
        doAutoTest();
    }
    
    @Test
    public void KDoc() {
        doAutoTest();
    }
    
    @Test
    public void LambdaArrow() {
        doAutoTest();
    }
    
    @Test
    public void LoopParameterWithExplicitType() {
        doAutoTest();
    }
    
    @Test
    public void Multideclaration() {
        doAutoTest();
    }
    
    @Test
    public void MultilineFunctionLiteral() {
        doAutoTest();
    }
    
    @Test
    public void MultilineFunctionLiteralWithParams() {
        doAutoTest();
    }
    
    @Test
    public void NewLineForRBrace() {
        doAutoTest();
    }
    
    @Test
    public void ObjectInBody() {
        doAutoTest();
    }
    
    @Test
    public void ParameterDocComments() {
        doAutoTest();
    }
    
    @Test
    public void Parameters() {
        doAutoTest();
    }
    
    @Test
    public void PrimaryConstructor() {
        doAutoTest();
    }
    
    @Test
    public void PropertyAccessors() {
        doAutoTest();
    }
    
    @Test
    public void PropertyTypeParameterList() {
        doAutoTest();
    }
    
    @Test
    public void PropertyWithInference() {
        doAutoTest();
    }
    
    @Test
    public void ReferenceExpressionFunctionLiteral() {
        doAutoTest();
    }
    
    @Test
    public void RemoveSpacesAroundOperations() {
        doAutoTest();
    }
    
    @Test
    public void ReturnExpression() {
        doAutoTest();
    }
    
    @Test
    public void RightBracketOnNewLine() {
        doAutoTest();
    }
    
    @Test
    public void SaveSpacesInDocComments() {
        doAutoTest();
    }
    
    @Test
    public void SecondaryConstructors() {
        doAutoTest();
    }
    
    @Test
    public void secondaryCtorLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void SingleLineFunctionLiteral() {
        doAutoTest();
    }
    
    @Test
    public void SpaceAroundExtendColon() {
        doAutoTest();
    }
    
    @Test
    public void SpaceAroundExtendColonInObjects() {
        doAutoTest();
    }
    
    @Test
    public void SpaceAroundExtendColonInSecondaryCtr() {
        doAutoTest();
    }
    
    @Test
    public void SpaceBeforeFunctionLiteral() {
        doAutoTest();
    }
    
    @Test
    public void SpacedInsideParans() {
        doAutoTest();
    }
    
    @Test
    public void SpacesAroundOperations() {
        doAutoTest();
    }
    
    @Test
    public void SpacesAroundUnaryOperations() {
        doAutoTest();
    }
    
    @Test
    public void SpacesInDeclarations() {
        doAutoTest();
    }
    
    @Test
    public void SpacesInQualifiedExpressions() {
        doAutoTest();
    }
    
    @Test
    public void TryCatchLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void UnnecessarySpacesInParametersLists() {
        doAutoTest();
    }
    
    @Test
    public void ValVarSpaces() {
        doAutoTest();
    }
    
    @Test
    public void When() {
        doAutoTest();
    }
    
    @Test
    public void WhenArrow() {
        doAutoTest();
    }
    
    @Test
    public void WhenEntryExpr() {
        doAutoTest();
    }
    
    @Test
    public void WhenLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void WhenLinesBeforeLbrace() {
        doAutoTest();
    }
    
    @Test
    public void WhileLineBreak() {
        doAutoTest();
    }
    
    @Test
    public void WhileOnNewLine() {
        doAutoTest();
    }
    
    @Test
    public void WhileSpacing() {
        doAutoTest();
    }
}