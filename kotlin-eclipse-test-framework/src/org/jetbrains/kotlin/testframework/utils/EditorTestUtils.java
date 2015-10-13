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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.junit.Assert;
import org.junit.ComparisonFailure;

public class EditorTestUtils {
    
    public static IEditorPart openInEditor(IFile file) throws PartInitException {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        
        FileEditorInput fileEditorInput = new FileEditorInput(file);
        IEditorDescriptor defaultEditor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
        
        return page.openEditor(fileEditorInput, defaultEditor.getId());
    }
    
    public static void assertByEditorWithErrorMessage(JavaEditor activeEditor, String expected, String message) {
        StyledText editorTextWidget = activeEditor.getViewer().getTextWidget();
		assertByStringWithEditorWidgetAndErrorMessage(EditorUtil.getSourceCode(activeEditor), expected, editorTextWidget, message);
    }
    
    public static void assertByEditor(JavaEditor activeEditor, String expected) {
    	assertByEditorWithErrorMessage(activeEditor, expected, "");
    }
    
    private static void assertByStringWithEditorWidgetAndErrorMessage(String actual, String expected, StyledText editorTextWidget, String errorMessage) {
        int caretOffset = editorTextWidget.getCaretOffset();
        Point selection = editorTextWidget.getSelection();
    	expected = expected.replaceAll(KotlinEditorTestCase.BREAK_TAG, System.lineSeparator());
    	int selectionStartOffset = selection.x;
        int selectionEndOffset = selection.y;
        if (expected.contains(KotlinEditorTestCase.CARET_TAG) && caretOffset != -1) {
            actual = actual.substring(0, caretOffset) + KotlinEditorTestCase.CARET_TAG + actual.substring(caretOffset);
            int caretTagLength = KotlinEditorTestCase.CARET_TAG.length();
            if (selectionStartOffset > caretOffset) {
				selectionStartOffset += caretTagLength;
            }
            if (selectionEndOffset >= caretOffset) {
            	selectionEndOffset += caretTagLength;
            }
        }
        // caret tag is expected to be absent if selection is present or to be within selection
        if (expected.contains(KotlinEditorTestCase.SELECTION_TAG_OPEN) && selection.x != selection.y) {
        	StringBuilder taggedBuilder = new StringBuilder(actual);
        	//insert closing tag first, because then there's no need to recalc end offset
        	taggedBuilder.insert(selectionEndOffset, KotlinEditorTestCase.SELECTION_TAG_CLOSE);
        	taggedBuilder.insert(selectionStartOffset, KotlinEditorTestCase.SELECTION_TAG_OPEN);
			actual = taggedBuilder.toString();
        }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
        try {
            Assert.assertEquals(LineEndUtil.removeAllCarriageReturns(expected), LineEndUtil.removeAllCarriageReturns(actual));
        } catch (ComparisonFailure e) {
            throw new ComparisonFailure(errorMessage, e.getExpected(), e.getActual());
        }
    }
}