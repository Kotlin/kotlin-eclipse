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
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil;
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProcessor;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionProposal;

import com.google.common.collect.Lists;

public class KotlinCorrectionProcessor implements IQuickAssistProcessor {
    
    private final AbstractTextEditor editor;
    private final KotlinQuickAssistProcessor kotlinQuickAssistProcessor;
    
    public KotlinCorrectionProcessor(AbstractTextEditor editor) {
        this.editor = editor;
        kotlinQuickAssistProcessor = new KotlinQuickAssistProcessor();
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public boolean canFix(Annotation annotation) {
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput());
        
        IMarker marker;
        IFile file = EditorUtil.getFile(editor);
        if (file != null) {
            Position position = annotationModel.getPosition(annotation);
            marker = DiagnosticAnnotationUtil.INSTANCE.getMarkerByOffset(file, position.getOffset());
        } else {
            marker = null;
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
        }

        return marker != null ? IDE.getMarkerHelpRegistry().hasResolutions(marker) : false;
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        return true;
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
        List<ICompletionProposal> completionProposals = new ArrayList<ICompletionProposal>();
        
        try {
            IMarker marker;
            IFile file = EditorUtil.getFile(editor);
            if (file != null) {
                int caretOffset = invocationContext.getOffset();
                marker = DiagnosticAnnotationUtil.INSTANCE.getMarkerByOffset(file, caretOffset);
            } else {
                marker = null;
                KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
            }

            if (marker != null) {
                for (IMarkerResolution markerResolution : IDE.getMarkerHelpRegistry().getResolutions(marker)) {
                    completionProposals.add(new KotlinMarkerResolutionProposal(marker, markerResolution));
                }
            } 

            completionProposals.addAll(collectQuickAssistProposals());
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }

        return completionProposals.toArray(new ICompletionProposal[completionProposals.size()]);
    }
    
    private List<ICompletionProposal> collectQuickAssistProposals() throws CoreException {
        List<ICompletionProposal> proposals = Lists.newArrayList();
        for (ICompletionProposal proposal : kotlinQuickAssistProcessor.getAssists(null, null)) {
            proposals.add(proposal);
        }
        
        return proposals;
    }
}