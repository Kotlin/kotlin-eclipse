package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionProposal;
import org.jetbrains.kotlin.utils.EditorUtil;

public class KotlinQuickAssistProcessor implements IQuickAssistProcessor {
    
    private final AbstractTextEditor editor;
    
    public KotlinQuickAssistProcessor(AbstractTextEditor editor) {
        this.editor = editor;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public boolean canFix(Annotation annotation) {
        return true;
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
            if (marker == null) {
                return Collections.emptyList().toArray(new ICompletionProposal[0]);
            }
            IMarkerResolution[] markerResolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
            
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