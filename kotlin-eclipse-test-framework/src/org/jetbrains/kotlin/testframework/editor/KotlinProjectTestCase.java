package org.jetbrains.kotlin.testframework.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

public class KotlinProjectTestCase {
    
    private static TestJavaProject testJavaProject;
    
    protected TextEditorTest testEditor = null;
    
    @Before
    public void beforeTest() {
        KotlinTestUtils.refreshWorkspace();
    }
    
    @After
    public void afterTest() {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
        
        if (testJavaProject != null) {
            testJavaProject.clean();
        }
    }
    
    @AfterClass
    public static void afterAllTests() {
        if (testJavaProject != null) {
            testJavaProject.clean();
            testJavaProject.setDefaultSettings();
        }
        
        try {
            IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            for (IProject project : projects) {
                project.delete(true, true, null);
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected KotlinFileEditor getEditor() {
        return (KotlinFileEditor) testEditor.getEditor();
    }
    
    protected void configureProject() {
        configureProject(TextEditorTest.TEST_PROJECT_NAME);
    }
    
    protected void configureProject(String projectName) {
        configureProject(projectName, null);
    }
    
    protected void configureProject(String projectName, String location) {
        testJavaProject = new TestJavaProject(projectName, location);
    }
    
    protected void configureProjectWithStdLib() {
        configureProjectWithStdLib(TextEditorTest.TEST_PROJECT_NAME);
    }
    
    protected void configureProjectWithStdLib(String projectName) {
        configureProjectWithStdLib(projectName, null);
    }
    
    protected void configureProjectWithStdLib(String projectName, String location) {
        configureProject(projectName, location);
        
        try {
            testJavaProject.addKotlinRuntime();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    public IFile createSourceFile(String pkg, String fileName, String content) {
        try {
            return testJavaProject.createSourceFile(pkg, fileName, content);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    public IFile createSourceFile(String fileName, String content) {
        return createSourceFile("", fileName, content);
    }
    
    protected TextEditorTest configureEditor(String fileName, String content) {
        return configureEditor(fileName, content, "");
    }
    
    protected TextEditorTest configureEditor(String fileName, String content, String pckgName) {
        TextEditorTest testEditor = new TextEditorTest(testJavaProject);
        testEditor.createEditor(fileName, content, pckgName);
        
        this.testEditor = testEditor;
        
        return testEditor;
    }
    
    protected TestJavaProject getTestProject() {
        return testJavaProject;
    }
}
