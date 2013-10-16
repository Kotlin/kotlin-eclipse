package org.jetbrains.kotlin.ui.tests.editors;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.utils.TestJavaProject;
import org.junit.Test;

public class KotlinCustomLocationBugTest extends KotlinEditorTestCase {

	private static final String CUSTOM_LOCATION_TEST_PROJECT_NAME = "Test Project";
	private static final String CUSTOM_LOCATION_TEST_PROJECT_LOCATION = "custom_location/custom_test_project";

	@Test
	public void testGetSrcDirectories() throws JavaModelException {
		TestJavaProject testProject = new TestJavaProject(CUSTOM_LOCATION_TEST_PROJECT_NAME, CUSTOM_LOCATION_TEST_PROJECT_LOCATION);
		
		IJavaProject javaProject = testProject.getJavaProject();
		List<File> files =  ProjectUtils.getSrcDirectories(javaProject);
	
		IPath workspaceRootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		IPath expectedSourcePath = workspaceRootPath.append(CUSTOM_LOCATION_TEST_PROJECT_LOCATION).append(TestJavaProject.SRC_FOLDER);
		
		Assert.assertEquals(1, files.size());
		Assert.assertEquals(expectedSourcePath.toFile(), files.get(0));
	}
	
}
