package org.jetbrains.kotlin.testframework.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import junit.framework.Assert;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class TextEditorTest {
	
	public static String CARET = "<caret>";

	private final static String PROJECT_NAME = "test_project";
	private TestJavaProject testProject;
	private JavaEditor editor;
	
	public TextEditorTest() {
		testProject = new TestJavaProject(PROJECT_NAME);
		
		editor = null;
	}
	
	public TestJavaProject getJavaProject() {
		return testProject;
	}
	
	public JavaEditor createEditor(String name, String content) {
		if (editor == null) {
			try {
				IFile file = testProject.createSourceFile("testing", name, "");
				
				editor = (JavaEditor) EditorTestUtils.openInEditor(file);
				
				setText(content);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		return editor;
	}
	
	public void type(char c) {
		Event e = new Event();
		e.character = c;
		if (editor == null) {
			createEditor("Test.kt", "");
		}
		
		e.widget = editor.getViewer().getTextWidget();
		
		e.widget.notifyListeners(SWT.KeyDown, e);
	}
	
	public void setText(String text) {
		int cursor = -1;
		if (text.contains(CARET)) {
			cursor = text.indexOf(CARET);
			text = text.replaceAll(CARET, "");
		}
		
		editor.getViewer().getDocument().set(text);
		
		if (cursor != -1) {
			editor.setHighlightRange(cursor, 0, true);
			editor.setFocus();
		}
	}
	
	public void typeEnter() {
		type('\n');
	}
	
	public void assertByEditor(String expected) {
		String actual = editor.getViewer().getDocument().get();
		
		if (expected.contains(CARET)) {
			int caretOffset = editor.getViewer().getTextWidget().getCaretOffset();
			actual = actual.substring(0, caretOffset) + CARET + actual.substring(caretOffset);
		}
		
		String expectedWithoutCR =  expected.replaceAll("\r", "");
		String actualWithoutCR = actual.replaceAll("\r", "");
		
		Assert.assertEquals(expectedWithoutCR, actualWithoutCR);
	}
	
	public void save() {
		editor.doSave(null);
	}
	
	public void close() {
		editor.close(false);
	}
	
	public IFile getEditingFile() {
		return (IFile) editor.getEditorInput().getAdapter(IFile.class);
	}
	
	public String getEditorInput() {
		return editor.getViewer().getDocument().get();
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
}
