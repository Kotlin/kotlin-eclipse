package org.jetbrains.kotlin.ui.tests.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;
import org.jetbrains.kotlin.testframework.utils.SourceFile;

public class KotlinUnresolvedClassFixTestCase extends KotlinEditorTestCase {

	public void doTest(String input, List<SourceFile> files, String expected) {
		testEditor = configureEditor("Test.kt", input);

		if (files != null) {
			for (SourceFile file : files) {
				createSourceFile(file.getName(), file.getContent());
			}
		}

		testEditor.save();
		joinBuildThread();

		try {
			IMarker[] markers = testEditor.getEditingFile().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			List<IMarkerResolution> resolutions = collectResolutions(markers);
			for (IMarkerResolution resolution : resolutions) {
				resolution.run(null);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

		EditorTestUtils.assertByEditor(testEditor.getEditor(), expected);
	}

	private List<IMarkerResolution> collectResolutions(IMarker[] markers) {
		List<IMarkerResolution> resolutions = new ArrayList<>();
		for (IMarker marker : markers) {
			if (IDE.getMarkerHelpRegistry().hasResolutions(marker)) {
				IMarkerResolution[] markerResolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
		        if (markerResolutions.length > 0) {
		        	resolutions.add(markerResolutions[0]);
		        }
			}
		}

		return resolutions;
	}
}