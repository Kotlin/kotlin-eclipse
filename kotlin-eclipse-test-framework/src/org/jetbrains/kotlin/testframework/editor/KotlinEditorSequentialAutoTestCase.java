package org.jetbrains.kotlin.testframework.editor;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;

public abstract class KotlinEditorSequentialAutoTestCase extends KotlinEditorAutoTestCase {
    
    @Override
    protected void doSingleFileAutoTest(String testPath) {
        throw new UnsupportedOperationException("Single-file tests are not supported for this case");
    }
    
    abstract protected void performSingleOperation();
    
    abstract protected String getInitialFileName();
    
    private TextEditorTest testEditor;
    
    protected TextEditorTest getTestEditor() {
        return testEditor;
    }
    
    @Override
    protected void doMultiFileAutoTest(File testFolder) {
        testEditor = configureEditor(getInitialFileName(), getInitialFileContent(testFolder));
        
        ArrayList<String> afterFilesPaths = getAfterFilesPaths(testFolder);
        
        JavaEditor activeEditor = (JavaEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        
        for (String afterFilePath : afterFilesPaths) {
            String afterFileContent = getAfterFileContent(afterFilePath);
            performSingleOperation();
            String errorMessage = String.format("Comparison error: %s", afterFilePath);
            EditorTestUtils.assertByEditorWithErrorMessage(activeEditor, afterFileContent, errorMessage);
        }
    }
    
    protected String getInitialFileContent(File testFolder) {
        return KotlinTestUtils.getText(testFolder.getAbsolutePath() + File.separator + getInitialFileName());
    }
    
    abstract protected ArrayList<String> getAfterFilesPaths(File testFolder);
    
    abstract protected String getAfterFileContent(String afterFilePath);
}
