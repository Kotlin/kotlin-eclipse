package org.jetbrains.kotlin.ui.tests.refactoring.rename;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinRenameTest extends KotlinRenameTestCase {
    @Test
    public void testSimple() {
        doTest("testData/refactoring/rename/simple/info.test");
    }
    
    @Test
    public void testAutomaticRenamer() {
        doTest("testData/refactoring/rename/automaticRenamer/simple.test");
    }
    
    @Test
    public void testRenameJavaClass() {
        doTest("testData/refactoring/rename/renameJavaClass/renameJavaClass.test");
    }
    
    @Test
    public void testRenameJavaClassSamePackage() {
        doTest("testData/refactoring/rename/renameJavaClassSamePackage/renameJavaClassSamePackage.test");
    }
    
    @Test
    public void testRenameJavaInterface() {
        doTest("testData/refactoring/rename/renameJavaInterface/renameJavaInterface.test");
    }
    
    @Ignore("Temporary disable as a bug")
    @Test
    public void testRenameJavaKotlinOverridenMethod() {
        doTest("testData/refactoring/rename/renameJavaMethod/kotlinOverridenMethod.test");
    }

    @Test
    public void testRenameKotlinClass() {
        doTest("testData/refactoring/rename/renameKotlinClass/kotlinClass.test");
    }
    
    @Test
    public void testRenameKotlinMethod() {
        doTest("testData/refactoring/rename/renameKotlinMethod/renameKotlinMethod.test");
    }
    
    @Test
    public void testRenameKotlinTopLevelFun() {
        doTest("testData/refactoring/rename/renameKotlinTopLevelFun/renameKotlinTopLevelFun.test");
    }
    
    @Test
    public void testRenameJavaStaticMethod() {
        doTest("testData/refactoring/rename/renameJavaStaticMethod/renameJavaStaticMethod.test");
    }
    
    @Test
    public void testRenameKotlinClassByConstructorReference() {
        doTest("testData/refactoring/rename/renameKotlinClassByConstructorRef/renameKotlinClassByConstructorRef.test");
    }
    
    @Test
    public void testRenameKotlinClassFromJava() {
        doTest("testData/refactoring/rename/renameKotlinClassFromJava/renameKotlinClassFromJava.test");
    }
    
    @Test
    public void testRenameKotlinMethodFromJava() {
        doTest("testData/refactoring/rename/renameKotlinMethodFromJava/renameKotlinMethodFromJava.test");
    }
    
    @Test
    public void testRenameKotlinTopLevelFunFromJava() {
        doTest("testData/refactoring/rename/renameKotlinTopLevelFunFromJava/renameKotlinTopLevelFunFromJava.test");
    }
}