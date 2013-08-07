package org.jetbrains.kotlin.testframework.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

public class TextEditorTest {
	
	public static String CARET = "<caret>";

	private final static String PROJECT_NAME = "test_project";
	private TestJavaProject testProject;
	private JavaEditor editor;
	
	public TextEditorTest() {
		testProject = new TestJavaProject(PROJECT_NAME);
		
		editor = null;
	}
	
	public TestJavaProject getTestJavaProject() {
		return testProject;
	}
	
	public JavaEditor createEditor(String name, String content) {
		if (editor == null) {
			try {
				int cursor = getCursorPosition(content);
				content = content.replaceAll(CARET, "");
				
				IFile file = testProject.createSourceFile("testing", name, content);
				editor = (JavaEditor) EditorTestUtils.openInEditor(file);
				setCaret(cursor);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		return editor;
	}
	
	private int getCursorPosition(String content) {
		int cursor = -1;
        if (content.contains(CARET)) {
            cursor = content.indexOf(CARET);
        }
        
        return cursor;
	}
	
	public void type(char c) {
		Event e = new Event();
		e.character = c;
		e.widget = editor.getViewer().getTextWidget();
		e.widget.notifyListeners(SWT.KeyDown, e);
	}
	
	public void accelerateOpenDeclaration() {
		((KotlinEditor) editor).getAction("OpenEditor").run();
	}
	
	public void setCaret(int offset) {
		if (offset > -1) {
			editor.setHighlightRange(offset, 0, true);
			editor.setFocus();
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
	
	public JavaEditor getEditor() {
		return editor;
	}
}
