package org.jetbrains.kotlin.testframework.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.utils.EditorUtil;

public class TextEditorTest {
	
	public static String CARET = "<caret>";

	public static final String TEST_PROJECT_NAME = "test_project";
	public static final String TEST_PACKAGE_NAME = "testing";
	private TestJavaProject testProject;
	private JavaEditor editor;
	
	public TextEditorTest() {
		this(TEST_PROJECT_NAME);
	}
	
	public TextEditorTest(String projectName) {
		testProject = new TestJavaProject(projectName);
		
		editor = null;
	}
	
	public TestJavaProject getTestJavaProject() {
		return testProject;
	}
	
	public JavaEditor createEditor(String name, String content) {
		return createEditor(name, content, TEST_PACKAGE_NAME);
	}
	
	public JavaEditor createEditor(String name, String content, String packageName) {
		if (editor == null) {
			try {
				int cursor = getCursorPosition(content);
				content = content.replaceAll(CARET, "");
				
				IFile file = testProject.createSourceFile(packageName, name, content);
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
		return EditorUtil.getFile(editor);
	}
	
	public String getEditorInput() {
		return EditorUtil.getSourceCode(editor);
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
