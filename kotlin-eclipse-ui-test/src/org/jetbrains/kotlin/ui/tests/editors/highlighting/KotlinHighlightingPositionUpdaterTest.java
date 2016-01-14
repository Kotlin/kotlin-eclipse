/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.tests.editors.highlighting;

import org.junit.Test;

public class KotlinHighlightingPositionUpdaterTest extends KotlinHighlightingPositionUpdaterTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "highlighting/positionUpdater";
    }
    
    @Test
    public void afterFunctionName() {
        doAutoTest();
    }
    
    @Test
    public void beforeFunctionName() {
        doAutoTest();
    }
    
    @Test
    public void illegalCharactersAfter() {
        doAutoTest();
    }
    
    @Test
    public void insideHighlightedPosition() {
        doAutoTest();
    }
    
    @Test
    public void illegalCharactersBefore() {
        doAutoTest();
    }
    
    @Test
    public void beforeHighlightedPosition() {
        doAutoTest();
    }
    
    @Test
    public void afterHighlightedPosition() {
        doAutoTest();
    }
    
    @Test
    public void illegalCharactersBeforeHighlightedPosition() {
        doAutoTest();
    }
}
