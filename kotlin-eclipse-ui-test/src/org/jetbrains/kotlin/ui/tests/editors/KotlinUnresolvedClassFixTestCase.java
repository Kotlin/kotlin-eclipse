/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
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
import org.jetbrains.kotlin.testframework.utils.SourceFileData;

public abstract class KotlinUnresolvedClassFixTestCase extends KotlinEditorTestCase {

	public void doTest(String input, List<SourceFileData> files, String expected) {
		testEditor = configureEditor("Test.kt", input);

		if (files != null) {
			for (SourceFileData data : files) {
				createSourceFile(data.getFileName(), data.getContent());
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