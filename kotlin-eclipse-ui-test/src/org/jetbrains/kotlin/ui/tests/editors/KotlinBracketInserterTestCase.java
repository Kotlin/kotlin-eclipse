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

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.junit.Before;

public abstract class KotlinBracketInserterTestCase extends KotlinProjectTestCase {
    @Before
    public void configure() {
        configureProject();
    }
    
    protected void doTest(String input, char element, String expected) {
        TextEditorTest testEditor = configureEditor("Test.kt", input);
        testEditor.type(element);
        
        EditorTestUtils.assertByEditor(testEditor.getEditor(), expected);
    }
}
