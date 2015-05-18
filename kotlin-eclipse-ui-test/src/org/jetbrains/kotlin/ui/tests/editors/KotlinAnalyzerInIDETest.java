/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.tests.editors;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinAnalyzerInIDETest extends KotlinAnalyzerInIDETestCase {
	@Test
    public void unresolvedPackageType() {
        doAutoTest();
    }
	
    @Test
    public void analyzerHasKotlinRuntime() {
        doAutoTest();
    }
    
    @Test
    public void checkAnalyzerFoundError() {
        doAutoTest();
    }
    
    @Test
    @Ignore
    public void analyzerHasKotlinAnnotations() {
        doAutoTest();
    }
    
    @Test
    public void javaFromKotlin() {
        doAutoTest();
    }
    
    @Test
    public void kotlinFromJava() {
        doAutoTest();
    }
    
    @Test
    public void kotlinInpackageFromJava() {
        doAutoTest();
    }
    
    @Ignore
    @Test
    public void kotlinJavaKotlin() {
        doAutoTest();
    }
    
    @Test
    public void kotlinWithErrorsFromJava() {
        doAutoTest();
    }
    
    @Test
    public void checkTestsFoundJavaError() {
        doAutoTest();
    }
    
    @Test
    public void classObjectFromJava() {
        doAutoTest();
    }
    
    @Test
    public void packageLevelFunctionsFromJava() {
        doAutoTest();
    }
    
    @Test
    public void packageLevelPropertiesFromJava() {
        doAutoTest();
    }
    
    @Test
    public void checkExistancePackageLevelFunctions() {
    	doAutoTest();
    }
}
