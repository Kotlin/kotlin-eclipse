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
package org.jetbrains.kotlin.testframework.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.junit.Assert;

public class EditorTestUtils {
    
    public static IEditorPart openInEditor(IFile file) throws PartInitException {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        
        FileEditorInput fileEditorInput = new FileEditorInput(file);
        IEditorDescriptor defaultEditor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
        
        return page.openEditor(fileEditorInput, defaultEditor.getId());
    }
    
    public static void assertByEditor(JavaEditor activeEditor, String expected) {
    	assertByStringWithOffset(EditorUtil.getSourceCode(activeEditor), expected, activeEditor.getViewer().getTextWidget().getCaretOffset());
    }
    
    public static void assertByStringWithOffset(String actual, String expected) {
        assertByStringWithOffset(actual, expected, -1);        
    }
    
    private static void assertByStringWithOffset(String actual, String expected, int caretOffset) {
        expected = expected.replaceAll(KotlinEditorTestCase.BREAK_TAG, System.lineSeparator());
        if (expected.contains(KotlinEditorTestCase.CARET_TAG) && caretOffset != -1) {
            actual = actual.substring(0, caretOffset) + KotlinEditorTestCase.CARET_TAG + actual.substring(caretOffset);
        }
        
        Assert.assertEquals(removeCarriegeReturns(expected), removeCarriegeReturns(actual));
    }
    
    private static String removeCarriegeReturns(String s) {
        return s.replaceAll("\r", "");
    }
}