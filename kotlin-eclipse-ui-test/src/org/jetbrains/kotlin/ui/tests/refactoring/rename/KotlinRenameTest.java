package org.jetbrains.kotlin.ui.tests.refactoring.rename;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinRenameTest extends KotlinRenameTestCase {
    
    @Ignore
    @Test
    public void testSimple() {
        doTest("testData/refactoring/rename/simple/info.test");
    }
    
    @Ignore
    @Test
    public void testAutomaticRenamer() {
        doTest("testData/refactoring/rename/automaticRenamer/simple.test");
    }
    
    @Ignore
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

    @Ignore
    @Test
    public void testRenameKotlinClass() {
        doTest("testData/refactoring/rename/renameKotlinClass/kotlinClass.test");
    }
    
    @Ignore
    @Test
    public void testRenameKotlinMethod() {
        doTest("testData/refactoring/rename/renameKotlinMethod/renameKotlinMethod.test");
    }
    
    @Ignore
    @Test
    public void testRenameKotlinTopLevelFun() {
        doTest("testData/refactoring/rename/renameKotlinTopLevelFun/renameKotlinTopLevelFun.test");
    }
    
    @Test
    public void testRenameJavaStaticMethod() {
        doTest("testData/refactoring/rename/renameJavaStaticMethod/renameJavaStaticMethod.test");
    }
    
    @Ignore
    @Test
    public void testRenameKotlinClassByConstructorReference() {
        doTest("testData/refactoring/rename/renameKotlinClassByConstructorRef/renameKotlinClassByConstructorRef.test");
    }
    
    @Ignore
    @Test
    public void testRenameKotlinClassFromJava() {
        doTest("testData/refactoring/rename/renameKotlinClassFromJava/renameKotlinClassFromJava.test");
    }
    
    @Ignore
    @Test
    public void testRenameKotlinMethodFromJava() {
        doTest("testData/refactoring/rename/renameKotlinMethodFromJava/renameKotlinMethodFromJava.test");
    }
    
    @Ignore
    @Test
    public void testRenameKotlinTopLevelFunFromJava() {
        doTest("testData/refactoring/rename/renameKotlinTopLevelFunFromJava/renameKotlinTopLevelFunFromJava.test");
    }
    
    @Ignore
    @Test
    public void testRenameClassInScript() {
        doTest("testData/refactoring/rename/scripts/renameClassInScript/info.test");
    }
    
    @Ignore
    @Test
    public void testRenameFunctionInScript() {
        doTest("testData/refactoring/rename/scripts/renameFunctionInScript/info.test");
    }
    
    @Ignore
    @Test
    public void testRenamePropertyInScript() {
        doTest("testData/refactoring/rename/scripts/renamePropertyInScript/info.test");
    }
    
    @Test
    public void testRenameInScriptLocally() {
        doTest("testData/refactoring/rename/scripts/renameInScriptLocally/info.test");
    }
}