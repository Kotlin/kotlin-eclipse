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
package org.jetbrains.kotlin.testframework.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.KotlinFormatAction;
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction;
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectEnclosingAction;
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectNextAction;
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectPreviousAction;

public class TextEditorTest {
    
    public static final String TEST_PROJECT_NAME = "test_project";
    public static final String TEST_PACKAGE_NAME = "testing";
    private TestJavaProject testProject;
    private JavaEditor editor = null;
    
    public TextEditorTest() {
        this(TEST_PROJECT_NAME);
    }
    
    public TextEditorTest(String projectName) {
        testProject = new TestJavaProject(projectName);
    }
    
    public TextEditorTest(TestJavaProject testProject) {
        this.testProject = testProject;
    }
    
    public TestJavaProject getTestJavaProject() {
        return testProject;
    }
    
    public IProject getEclipseProject() {
        return testProject.getJavaProject().getProject();
    }
    
    public JavaEditor createEditor(String name, String content) {
        return createEditor(name, content, TEST_PACKAGE_NAME);
    }
    
    public JavaEditor createEditor(String name, String content, String packageName) {
        if (editor == null) {
            try {
                int cursor = content.indexOf(KotlinEditorTestCase.CARET_TAG);
                content = content.replaceAll(KotlinEditorTestCase.CARET_TAG, "");
                
                int selectionStart = content.indexOf(KotlinEditorTestCase.SELECTION_TAG_OPEN);
                content = content.replaceAll(KotlinEditorTestCase.SELECTION_TAG_OPEN, "");
                
                int selectionEnd = content.indexOf(KotlinEditorTestCase.SELECTION_TAG_CLOSE);
                content = content.replaceAll(KotlinEditorTestCase.SELECTION_TAG_CLOSE, "");               
                
                IFile file = testProject.createSourceFile(packageName, name, content);
                editor = (JavaEditor) EditorTestUtils.openInEditor(file);
                setCaret(cursor);
                setSelection(selectionStart, selectionEnd - selectionStart);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        return editor;
    }    

	public void type(char c) {
        Event e = new Event();
        e.character = c;
        e.widget = editor.getViewer().getTextWidget();
        e.widget.notifyListeners(SWT.KeyDown, e);
    }
    
    public void runOpenDeclarationAction() {
        ((KotlinEditor) editor).getJavaEditor().getAction(KotlinOpenDeclarationAction.OPEN_EDITOR_TEXT).run();
    }
    
    public void runFormatAction() {
        ((KotlinEditor) editor).getJavaEditor().getAction(KotlinFormatAction.Companion.getFORMAT_ACTION_TEXT()).run();
    }
    
    public void runSelectEnclosingAction() {
        ((KotlinEditor) editor).getJavaEditor().getAction(KotlinSelectEnclosingAction.SELECT_ENCLOSING_TEXT).run();
    }
    
    public void runSelectPreviousAction() {
        ((KotlinEditor) editor).getJavaEditor().getAction(KotlinSelectPreviousAction.SELECT_PREVIOUS_TEXT).run();
    }
    
    public void runSelectNextAction() {
        ((KotlinEditor) editor).getJavaEditor().getAction(KotlinSelectNextAction.SELECT_NEXT_TEXT).run();
    }
    
    public int getCaretOffset() {
    	return ((ITextSelection) editor.getViewer().getSelectionProvider().getSelection()).getOffset();
    }
    
    public void setCaret(int offset) {
        if (offset > -1) {
            editor.setHighlightRange(offset, 0, true);
            editor.setFocus();
        }
    }
    
    public void setSelection(int selectionStart, int selectionLength) {
		if (selectionStart >= 0 && selectionLength >= 0) {
			editor.selectAndReveal(selectionStart, selectionLength);
		}
    }
    
    public void typeEnter() {
        type('\n');
    }
    
    public void save() {
        editor.doSave(null);
    }
    
    public void close() {
        editor.close(false);
    }
    
    public IFile getEditingFile() {
        return EditorUtil.getFile(editor);
    }
    
    public String getEditorInput() {
        return EditorUtil.getDocument(editor).get();
    }
    
    public void deleteEditingFile() {
        IFile editingFile = getEditingFile();
        close();
        try {
            editingFile.delete(true, null);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    public JavaEditor getEditor() {
        return editor;
    }
    
    public IDocument getDocument() {
    	return editor.getDocumentProvider().getDocument(editor.getEditorInput());
    }
}
