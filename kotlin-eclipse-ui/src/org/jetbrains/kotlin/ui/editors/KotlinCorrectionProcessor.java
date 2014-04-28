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
package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinAutoImportMarkerResolutionGenerator;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionProposal;
import org.jetbrains.kotlin.utils.EditorUtil;

public class KotlinCorrectionProcessor implements IQuickAssistProcessor {
    
    private final AbstractTextEditor editor;
    
    public KotlinCorrectionProcessor(AbstractTextEditor editor) {
        this.editor = editor;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public boolean canFix(Annotation annotation) {
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput());
        
        Position position = annotationModel.getPosition(annotation);
        try {
            IMarker marker = findMarkerAt(position.getOffset());
            return IDE.getMarkerHelpRegistry().hasResolutions(marker);
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
        
        return false;
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        return true;
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
        int caretOffset = invocationContext.getOffset();
        List<ICompletionProposal> completionProposals = new ArrayList<ICompletionProposal>();
        
        try {
            IMarker marker = findMarkerAt(caretOffset);
            IMarkerResolution[] markerResolutions = null; 
            if (marker != null) {
                markerResolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
            } else {
                KotlinAutoImportMarkerResolutionGenerator resolutionGenerator = new KotlinAutoImportMarkerResolutionGenerator();
                markerResolutions = resolutionGenerator.getResolutions(null);
            }
            
            for (IMarkerResolution markerResolution : markerResolutions) {
                completionProposals.add(new KotlinMarkerResolutionProposal(marker, markerResolution));
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        
        return completionProposals.toArray(new ICompletionProposal[completionProposals.size()]);
    }
    
    private IMarker findMarkerAt(int offset) throws CoreException {
        IFile file = EditorUtil.getFile(editor);
        
        IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        final int defaultOffset = -1;
        for (IMarker marker : markers) {
            int markerStart = marker.getAttribute(IMarker.CHAR_START, defaultOffset);
            int markerEnd = marker.getAttribute(IMarker.CHAR_END, defaultOffset);
            if (markerStart <= offset && markerEnd >= offset) {
                return marker;
            }
        }
        
        return null;
    }
}