package org.jetbrains.kotlin.ui.editors.quickfix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameMatchRequestor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.ui.editors.AnnotationManager;
import org.jetbrains.kotlin.ui.editors.DiagnosticAnnotation;

import com.intellij.openapi.util.TextRange;

public class KotlinMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {
    
    private static SearchEngine searchEngine = new SearchEngine();
    private static IMarkerResolution[] NO_RESOLUTIONS = new IMarkerResolution[] { };
    
    @Override
    public IMarkerResolution[] getResolutions(@Nullable IMarker marker) {
        if (!hasResolutions(marker)) {
            return NO_RESOLUTIONS;
        }
        String markedText = null;
        if (marker != null) {
            markedText = marker.getAttribute(AnnotationManager.MARKED_TEXT, null);
        }
        
        if (markedText == null) {
            JavaEditor activeEditor = (JavaEditor) getActiveEditor();
            
            if (activeEditor != null) {
                int caretOffset = activeEditor.getViewer().getTextWidget().getCaretOffset();
                DiagnosticAnnotation annotation = getAnnotationByOffset(caretOffset);
                if (annotation != null) {
                    markedText = annotation.getMarkedText();
                }
            }
        }
        
        if (markedText == null) {
            return NO_RESOLUTIONS;
        }
        
        List<IType> typeResolutions = findAllTypes(markedText);
        List<AutoImportMarkerResolution> markerResolutions = new ArrayList<AutoImportMarkerResolution>();
        for (IType type : typeResolutions) {
            markerResolutions.add(new AutoImportMarkerResolution(type));
        }
        
        return markerResolutions.toArray(new IMarkerResolution[markerResolutions.size()]);
    }
    
    @NotNull
    private List<IType> findAllTypes(@NotNull String typeName) {
        IJavaSearchScope scope = SearchEngine.createWorkspaceScope();
        List<IType> searchCollector = new ArrayList<IType>();
        TypeNameMatchRequestor requestor = new KotlinSearchTypeRequestor(searchCollector);
        try {
            searchEngine.searchAllTypeNames(null, 
                    SearchPattern.R_EXACT_MATCH, 
                    typeName.toCharArray(), 
                    SearchPattern.R_EXACT_MATCH, 
                    IJavaSearchConstants.TYPE, 
                    scope, 
                    requestor,
                    IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
                    null);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return searchCollector;
    }

    @Override
    public boolean hasResolutions(@Nullable IMarker marker) {
        JavaEditor activeEditor = (JavaEditor) getActiveEditor();
        
        if (activeEditor == null) {
            return false;
        }        
        
        int caretOffset = activeEditor.getViewer().getTextWidget().getCaretOffset();
        Annotation annotation = getAnnotationByOffset(caretOffset);
        
        if (annotation != null) {
            if (annotation instanceof DiagnosticAnnotation) {
                return ((DiagnosticAnnotation) annotation).quickFixable();
            }
        }
        
        if (marker != null) {
            return marker.getAttribute(AnnotationManager.IS_QUICK_FIXABLE, false);
        }
     
        return false;
    }
    
    @Nullable
    private DiagnosticAnnotation getAnnotationByOffset(int offset) {
        AbstractTextEditor editor = getActiveEditor();
        if (editor == null) {
            return null;
        }
        
        IDocumentProvider documentProvider = editor.getDocumentProvider();
        IAnnotationModel annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput());
        
        for (Iterator<?> i = annotationModel.getAnnotationIterator(); i.hasNext();) {
            Annotation annotation = (Annotation) i.next();
            if (annotation instanceof DiagnosticAnnotation) {
                DiagnosticAnnotation diagnosticAnnotation = (DiagnosticAnnotation) annotation;
                
                TextRange range = diagnosticAnnotation.getRange();
                if (range.getStartOffset() <= offset && range.getEndOffset() >= offset) {
                    return diagnosticAnnotation;
                }
            }
        }
        
        return null;
    }
    
    @Nullable
    private AbstractTextEditor getActiveEditor() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (workbenchWindow == null) {
            return null;
        }

        AbstractTextEditor editor = (AbstractTextEditor) workbenchWindow.getActivePage().getActiveEditor();
        
        return editor;
    }
}