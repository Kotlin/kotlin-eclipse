package org.jetbrains.kotlin.ui.tests.editors;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.eclipse.ui.PlatformUI;

public class KotlinOpenDeclarationTestCase extends KotlinEditorTestCase {
	
	protected void doTest(String inputFileName, String input, String expected) {
		doTest(inputFileName, input, null, expected);
	}
	
	protected void doTest(String inputFileName, String input, String referenceFileName, String referenceFile) {
		testEditor = configureEditor(inputFileName, input);
		
		String expected = referenceFile;
		if (referenceFileName != null) {
			createSourceFile(referenceFileName, referenceFile);
		}
		
		refreshWorkspace();
		joinBuildThread();
		
    	testEditor.accelerateOpenDeclaration();

		JavaEditor activeEditor = (JavaEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activeEditor instanceof KotlinEditor) {
			EditorTestUtils.assertByEditor((KotlinEditor) activeEditor, expected);
		} else {
			EditorTestUtils.assertByEditor(activeEditor, expected);
		}
	}
}
