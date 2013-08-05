package org.jetbrains.kotlin.ui.tests.editors;

import junit.framework.Assert;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.jetbrains.kotlin.testframework.editor.TextEditorTest;
import org.jetbrains.kotlin.testframework.utils.WorkspaceUtil;
import org.junit.After;
import org.junit.Before;

public class KotlinBracketInserterTestCase {

	private TextEditorTest testEditor;
	
	@After
	public void deleteEditingFile() {
		if (testEditor != null) {
			testEditor.deleteEditingFile();
		}
	}
	
	@Before
	public void refreshWorkspace() {
		WorkspaceUtil.refreshWorkspace();
		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, new NullProgressMonitor());
		} catch (OperationCanceledException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected void doTest(String input, char element, String expected) {
		testEditor = new TextEditorTest();
		testEditor.createEditor("Test.kt", input);
		
		testEditor.type(element);
		
		String actual = testEditor.getEditorInput();
		
		Assert.assertEquals(expected, actual);
	}
}
