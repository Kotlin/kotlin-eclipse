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
package org.jetbrains.kotlin.ui.editors.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class KotlinMarkerResolutionProposal implements ICompletionProposal {

    private final IMarkerResolution markerResolution;
    private final IMarker marker;
    
    public KotlinMarkerResolutionProposal(IMarker marker, IMarkerResolution markerResolution) {
        this.marker = marker;
        this.markerResolution = markerResolution;
    }
    
    @Override
    public void apply(IDocument document) {
        markerResolution.run(marker);
    }

    @Override
    public Point getSelection(IDocument document) {
        if (markerResolution instanceof ICompletionProposal) {
            return ((ICompletionProposal) markerResolution).getSelection(document);
        }
        
        return null;
    }

    @Override
    public String getAdditionalProposalInfo() {
        if (markerResolution instanceof IMarkerResolution2) {
            return ((IMarkerResolution2) markerResolution).getDescription();
        }
        if (markerResolution instanceof ICompletionProposal) {
            return ((ICompletionProposal) markerResolution)
                    .getAdditionalProposalInfo();
        }
        
        try {
            return "Problem description: " + marker.getAttribute(IMarker.MESSAGE);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }

    @Override
    public String getDisplayString() {
        return markerResolution.getLabel();
    }

    @Override
    public Image getImage() {
        if (markerResolution instanceof IMarkerResolution2) {
            return ((IMarkerResolution2) markerResolution).getImage();
        }
        if (markerResolution instanceof ICompletionProposal) {
            return ((ICompletionProposal) markerResolution).getImage();
        }
        
        return null;
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }
}