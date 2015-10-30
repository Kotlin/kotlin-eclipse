package org.jetbrains.kotlin.ui.tests.search;

import org.junit.Test;


public class KotlinFindReferencesTest extends KotlinFindReferencesTestCase {
    @Test
    public void testJKClassAllUsages() {
        doTest("testData/findUsages/java/JKClassAllUsages.0.java");
    }
    
    @Test
    public void testJKClassDerivedAnonymousObjects() {
        doTest("testData/findUsages/java/JKClassDerivedAnonymousObjects.0.java");
    }
    
    @Test
    public void testJKClassDerivedClasses() {
        doTest("testData/findUsages/java/JKClassDerivedClasses.0.java");
    }
    
    @Test
    public void testJKClassDerivedInnerClasses() {
        doTest("testData/findUsages/java/JKClassDerivedInnerClasses.0.java");
    }
    
    @Test
    public void testJKClassDerivedInnerObjects() {
        doTest("testData/findUsages/java/JKClassDerivedInnerObjects.0.java");
    }
    
    @Test
    public void testJKClassDerivedLocalClasses() {
        doTest("testData/findUsages/java/JKClassDerivedLocalClasses.0.java");
    }
    
    @Test
    public void testJKClassDerivedLocalObjects() {
        doTest("testData/findUsages/java/JKClassDerivedLocalObjects.0.java");
    }
    
    @Test
    public void testJKClassDerivedObjects() {
        doTest("testData/findUsages/java/JKClassDerivedObjects.0.java");
    }
    
    @Test
    public void testJKClassWithImplicitConstructorAllUsages() {
        doTest("testData/findUsages/java/JKClassWithImplicitConstructorAllUsages.0.java");
    }
    
    @Test
    public void testJKInnerClassAllUsages() {
        doTest("common_testData/ide/findUsages/java/findJavaClassUsages/JKInnerClassAllUsages.0.java");
    }
    
    @Test
    public void testJKNestedClassAllUsages() {
        doTest("testData/findUsages/java/JKNestedClassAllUsages.0.java");
    }
    
    @Test
    public void testJavaClassAllUsages() {
        doTest("common_testData/ide/findUsages/kotlin/findClassUsages/javaClassAllUsages.0.kt");
    }
    
    @Test
    public void testKotlinLocalClassUsages1() {
        doTest("common_testData/ide/findUsages/kotlin/findClassUsages/kotlinLocalClassUsages1.0.kt");
    }
    
    @Test
    public void testKotlinLocalClassUsages2() {
        doTest("common_testData/ide/findUsages/kotlin/findClassUsages/kotlinLocalClassUsages2.0.kt");
    }
    
    @Test
    public void testJavaMethodUsages() {
        doTest("testData/findUsages/kotlin/findFunctionUsages/javaMethodUsages.0.kt");
    }
    
    @Test
    public void testKotlinLocalMethodUsages1() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/kotlinLocalMethodUsages1.0.kt");
    }
    
    @Test
    public void testKotlinLocalMethodUsages2() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/kotlinLocalMethodUsages2.0.kt");
    }
    
    @Test
    public void testKotlinMethodUsages() {
        doTest("testData/findUsages/kotlin/findFunctionUsages/kotlinMethodUsages.0.kt");
    }
    
    @Test
    public void testKotlinMultiRefInImport() {
        doTest("testData/findUsages/kotlin/findFunctionUsages/kotlinMultiRefInImport.0.kt");
    }
    
    @Test
    public void testKotlinNestedClassMethodUsages() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/kotlinNestedClassMethodUsages.0.kt");
    }
    
    @Test
    public void testKotlinTopLevelMethodUsagesNoImport() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/kotlinTopLevelMethodUsagesNoImport.0.kt");
    }
    
    @Test
    public void testKotlinTraitImplThroughDelegate() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/kotlinTraitImplThroughDelegate.0.kt");
    }
    
    @Test
    public void testKotlinTraitNoImplThroughDelegate() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/kotlinTraitNoImplThroughDelegate.0.kt");
    }
    
    @Test
    public void testLocalClassMember() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/localClassMember.0.kt");
    }
    
    @Test
    public void testObjectExpressionDeepMember() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/objectExpressionDeepMember.0.kt");
    }
    
    @Test
    public void testObjectExpressionMember() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/objectExpressionMember.0.kt");
    }
    
    @Test
    public void testObjectExpressionMember2() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/objectExpressionMember2.0.kt");
    }
    
    @Test
    public void testSynthesizedFunction() {
        doTest("common_testData/ide/findUsages/kotlin/findFunctionUsages/synthesizedFunction.0.kt");
    }
    
    @Test
    public void testJavaObjectUsages() {
        doTest("testData/findUsages/kotlin/findObjectUsages/javaObjectUsages.0.kt");
    }
    
    @Test
    public void testKotlinLocalObjectUsages1() {
        doTest("common_testData/ide/findUsages/kotlin/findObjectUsages/kotlinLocalObjectUsages1.0.kt");
    }
    
    @Test
    public void testKotlinLocalObjectUsages2() {
        doTest("common_testData/ide/findUsages/kotlin/findObjectUsages/kotlinLocalObjectUsages2.0.kt");
    }
    
    @Test
    public void testKotlinNestedObjectUsages() {
        doTest("testData/findUsages/kotlin/findObjectUsages/kotlinNestedObjectUsages.0.kt");
    }
    
    @Test
    public void testKotlinObjectUsages() {
        doTest("testData/findUsages/kotlin/findObjectUsages/kotlinObjectUsages.0.kt");
    }
    
    @Test
    public void testKotlinConstructorParameterUsages() {
        doTest("common_testData/ide/findUsages/kotlin/findParameterUsages/kotlinConstructorParameterUsages.0.kt");
    }
    
    @Test
    public void testKotlinFunctionParameterUsages() {
        doTest("common_testData/ide/findUsages/kotlin/findParameterUsages/kotlinFunctionParameterUsages.0.kt");
    }
    
    @Test
    public void testJavaClassObjectPropertyUsages() {
        doTest("common_testData/ide/findUsages/kotlin/findPropertyUsages/javaClassObjectPropertyUsages.0.kt");
    }
    
    @Test
    public void testKotlinClassObjectPropertyUsage() {
        doTest("common_testData/ide/findUsages/kotlin/findPropertyUsages/kotlinClassObjectPropertyUsage.0.kt");
    }
    
    @Test
    public void testKotlinLocalPropertyUsages1() {
        doTest("common_testData/ide/findUsages/kotlin/findPropertyUsages/kotlinLocalPropertyUsages1.0.kt");
    }
    
    @Test
    public void testKotlinLocalPropertyUsages2() {
        doTest("common_testData/ide/findUsages/kotlin/findPropertyUsages/kotlinLocalPropertyUsages2.0.kt");
    }
    
    @Test
    public void testKotlinNestedClassPropertyUsages() {
        doTest("common_testData/ide/findUsages/kotlin/findPropertyUsages/kotlinNestedClassPropertyUsages.0.kt");
    }
    
    @Test
    public void testKotlinPrivatePropertyInClassObjectUsages() {
        doTest("common_testData/ide/findUsages/kotlin/findPropertyUsages/kotlinPrivatePropertyInClassObjectUsages.0.kt");
    }
    
    @Test
    public void testKotlinTopLevelPropertyUsages() {
        doTest("testData/findUsages/kotlin/findPropertyUsages/kotlinTopLevelPropertyUsages.0.kt");
    }
    
    
}