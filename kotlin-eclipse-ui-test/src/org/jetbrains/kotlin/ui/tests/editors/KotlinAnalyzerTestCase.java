package org.jetbrains.kotlin.ui.tests.editors;

import junit.framework.Assert;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;

public class KotlinAnalyzerTestCase extends KotlinEditorTestCase {

	protected void doTest(String input, String fileName) {
		testEditor = configureEditor(fileName, input);
		try {
			testEditor.save();
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, new NullProgressMonitor());
			} catch (OperationCanceledException | InterruptedException e) {
				e.printStackTrace();
			}
			
			String editorInput = insertTagsForErrors(testEditor.getEditorInput(), 
					testEditor.getEditingFile().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
			
			String editorInputWithoutCR = editorInput.toString().replaceAll("\r", "");
			input = input.replaceAll("\r", "");
			
			Assert.assertEquals(input, editorInputWithoutCR);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	} 
	
	private String insertTagsForErrors(String text, IMarker[] markers) throws CoreException {
		StringBuilder editorInput = new StringBuilder(text);
		int tagShift = 0;
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) {
				editorInput.insert((int) marker.getAttribute(IMarker.CHAR_START) + tagShift, ERR_TAG_OPEN);
				tagShift += ERR_TAG_OPEN.length();
				
				editorInput.insert((int) marker.getAttribute(IMarker.CHAR_END) + tagShift, ERR_TAG_CLOSE);
				tagShift += ERR_TAG_CLOSE.length();
			}
		}
		
		return editorInput.toString();
	}
}
