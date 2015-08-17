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
        doTest("testData/findUsages/java/JKInnerClassAllUsages.0.java");
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
}