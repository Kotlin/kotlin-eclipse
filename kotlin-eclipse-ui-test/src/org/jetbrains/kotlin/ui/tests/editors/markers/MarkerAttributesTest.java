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
package org.jetbrains.kotlin.ui.tests.editors.markers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.junit.Test;

public class MarkerAttributesTest extends KotlinParsingMarkersTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "markers/parsing";
    }
    
    @Override
    protected void performTest(String fileText, String expected) {
        try {
            IMarker[] markers = generateAndGetProblemMarkers(fileText);
            
            for (IMarker marker : markers) {
                assertThat(marker.getAttribute(IMarker.LOCATION), equalTo("line 3"));
                assertThat(marker.getAttribute(IMarker.LINE_NUMBER), equalTo(3));
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Test
    public void missingClosingBraceErrorTest() {
        doAutoTest();
    }
}
