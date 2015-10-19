package org.jetbrains.kotlin.ui.tests.editors.navigation;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.actions.OpenAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.junit.Before;

public abstract class JavaToKotlinNavigationTestCase extends KotlinNavigationTestCase {
    @Before
    public void before() {
        configureProjectWithStdLib();
    }
    
	@Override
	protected void doSingleFileAutoTest(String testPath) {
		throw new IllegalArgumentException("Tests with one file are not supported for this case");
	}

	@Override
	protected void performTest(String contentAfter) {
		KotlinTestUtils.joinBuildThread();
		
		JavaEditor editor = getTestEditor().getEditor();
		OpenAction openAction = new OpenAction(editor);
		openAction.run(new TextSelection(KotlinTestUtils.getCaret(editor), 0));
        
        JavaEditor activeEditor = (JavaEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        EditorTestUtils.assertByEditor(activeEditor, contentAfter);
	}
}
