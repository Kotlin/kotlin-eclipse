package org.jetbrains.kotlin.ui.tests.editors;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.junit.Test;

public class KotlinCustomLocationBugTest extends KotlinEditorTestCase {

	private static final String TEST_PROJECT_NAME = "Test Project";
	private static final String TEST_PROJECT_LOCATION = "custom_location/custom_test_project";

	@Test
	public void test() throws JavaModelException {
		TestJavaProject testProject = new TestJavaProject(TEST_PROJECT_NAME, TEST_PROJECT_LOCATION);
		
		List<File> files = testProject.getSrcDirectories();
	
		IPath workspaceRootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		IPath expectedSourcePath = workspaceRootPath.append(TEST_PROJECT_LOCATION).append(TestJavaProject.SRC_FOLDER);
		
		Assert.assertEquals(files.size(), 1);
		Assert.assertEquals(expectedSourcePath.toFile(), files.get(0));
	}
	
}
