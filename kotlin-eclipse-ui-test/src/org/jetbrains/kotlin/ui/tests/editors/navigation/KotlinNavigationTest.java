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
package org.jetbrains.kotlin.ui.tests.editors.navigation;

import org.junit.Before;
import org.junit.Test;

public class KotlinNavigationTest extends KotlinNavigationTestCase {
    @Before
    public void before() {
        configureProjectWithStdLib();
    }
    
	@Override
    protected String getTestDataRelativePath() {
        return "navigation";
    }
	
    @Test
    public void withinFileToClassNavigation() {
        doAutoTest();
    }
    
    @Test
    public void withinFileToMethodNavigation() {
        doAutoTest();
    }
    
    @Test
    public void withinFileFromConstructorToClassNavigation() {
        doAutoTest();
    }
    
    @Test
    public void toKotlinClassNavigation() {
        doAutoTest();
    }
    
    @Test
    public void toKotlinMethodNavigation() {
        doAutoTest();
    }
    
    @Test
    public void toJavaClassNavigation() {
        doAutoTest();
    }
    
    @Test
    public void toJavaMethodNavigation() {
        doAutoTest();
    }
    
    @Test
    public void fromSyntheticPropertyOnlyWithGetter() {
        doAutoTest();
    }
    
    @Test
    public void fromGetterSyntheticProperty() {
        doAutoTest();
    }
    
    @Test
    public void fromSetterSyntheticProperty() {
        doAutoTest();
    }
    
    @Test
    public void toJavaGetterMethod() {
        doAutoTest();
    }
}
