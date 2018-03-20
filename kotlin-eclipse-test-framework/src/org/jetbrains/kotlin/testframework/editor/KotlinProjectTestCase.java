package org.jetbrains.kotlin.testframework.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase.Separator;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

public class KotlinProjectTestCase {
    
    private static TestJavaProject testJavaProject;
    private int initialSpacesCount;
    private Separator initialSeparator;
    
    @Before
    public void beforeTest() {
        KotlinTestUtils.refreshWorkspace();
        
        initialSeparator = EditorsUI.getPreferenceStore().getBoolean(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS) ? Separator.SPACE : Separator.TAB;
        initialSpacesCount = EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, true);
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, 4);
    }
    
    @After
    public void afterTest() {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
        
        if (testJavaProject != null) {
            testJavaProject.clean();
        }
        
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_SPACES_FOR_TABS, (Separator.SPACE == initialSeparator));
        EditorsUI.getPreferenceStore().setValue(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH, initialSpacesCount);
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
            return testJavaProject.createSourceFile(pkg, fileName, KotlinEditorTestCase.removeTags(content));
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
    
    protected TextEditorTest configureEditor(String fileName, String content, String pkg) {
        TextEditorTest testEditor = new TextEditorTest(testJavaProject);
        testEditor.createEditor(fileName, content, pkg);
        
        return testEditor;
    }
    
    protected TestJavaProject getTestProject() {
        return testJavaProject;
    }
    
    protected void waitForEditorInitialization(TextEditorTest testEditor) throws OperationCanceledException, InterruptedException {
        String family = KotlinScriptEnvironment.Companion.constructFamilyForInitialization(testEditor.getEditingFile());
        Job.getJobManager().join(family, null);
    }
}
