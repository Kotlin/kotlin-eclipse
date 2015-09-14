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
package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions;

import org.junit.Test;

public class KotlinSpecifyTypeTest extends KotlinSpecifyTypeTestCase {
    
    @Test
    public void testBadCaretPosition() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/badCaretPosition.kt");
    }
    
    @Test
    public void testClassNameClashing() {
        doTest("testData/intentions/specifyType/ClassNameClashing.kt");
    }
    
    @Test
    public void testConstructor() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/constructor.kt");
    }
    
    @Test
    public void testEnumType() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/enumType.kt");
    }
    
    @Test
    public void testFunctionType() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/functionType.kt");
    }
    
    @Test
    public void testLambdaParam() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/lambdaParam.kt");
    }
    
    @Test
    public void testLoopParameter() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/loopParameter.kt");
    }
    
    @Test
    public void testPublicMember() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/publicMember.kt");
    }
    
    @Test
    public void testStringRedefined() {
        doTest("testData/intentions/specifyType/StringRedefined.kt");
    }
    
    @Test
    public void testTypeAlreadyProvided() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/typeAlreadyProvided.kt");
    }
    
    @Test
    public void testUnitType() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/unitType.kt");
    }
    
    @Test
    public void testUnknownType() {
        doTest("common_testData/ide/intentions/specifyTypeExplicitly/unknownType.kt");
    }
}
