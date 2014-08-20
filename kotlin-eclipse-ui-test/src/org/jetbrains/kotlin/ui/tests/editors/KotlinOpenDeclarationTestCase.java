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

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;

public abstract class KotlinOpenDeclarationTestCase extends KotlinEditorTestCase {
    
    public void doTest(String inputFileName, String input, String expected) {
        doTest(inputFileName, input, null, expected);
    }
    
    protected void doTest(String inputFileName, String input, String referenceFileName, String referenceFile) {
        testEditor = configureEditor(inputFileName, input);
        
        String expected = referenceFile;
        if (referenceFileName != null) {
            createSourceFile(referenceFileName, referenceFile);
        }
        
        testEditor.accelerateOpenDeclarationAction();
        
        JavaEditor activeEditor = (JavaEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        EditorTestUtils.assertByEditor(activeEditor, expected);
    }
}
