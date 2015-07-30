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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;

import com.google.common.base.Predicate;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;

public class DiagnosticAnnotationUtil {

    public static final DiagnosticAnnotationUtil INSTANCE = new DiagnosticAnnotationUtil();
    
    private DiagnosticAnnotationUtil() {
    }
    
    @NotNull
    public Map<IFile, List<DiagnosticAnnotation>> handleDiagnostics(@NotNull Diagnostics diagnostics) {
        Map<IFile, List<DiagnosticAnnotation>> annotations = new HashMap<IFile, List<DiagnosticAnnotation>>();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getTextRanges().isEmpty()) {
                continue;
            }
            
            VirtualFile virtualFile = diagnostic.getPsiFile().getVirtualFile();
            if (virtualFile == null) {
                continue;
            }
            
            IFile curFile = ResourcesPlugin.getWorkspace().getRoot().
                    getFileForLocation(new Path(virtualFile.getPath()));
            
            if (!annotations.containsKey(curFile)) {
                annotations.put(curFile, new ArrayList<DiagnosticAnnotation>());
            }
            
            DiagnosticAnnotation annotation = createKotlinAnnotation(diagnostic, curFile);
            annotations.get(curFile).add(annotation);
        }
        
        return annotations;
    }
    
    public void addParsingDiagnosticAnnotations(@NotNull IFile file, @NotNull Map<IFile, List<DiagnosticAnnotation>> annotations) {
        List<DiagnosticAnnotation> parsingAnnotations = createParsingDiagnosticAnnotations(file);
        
        if (annotations.containsKey(file)) {
            annotations.get(file).addAll(parsingAnnotations);
        } else {
            annotations.put(file, parsingAnnotations);
        }
    }
    
    @NotNull
    public List<DiagnosticAnnotation> createParsingDiagnosticAnnotations(@NotNull IFile file) {
        JetFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        List<DiagnosticAnnotation> result = new ArrayList<DiagnosticAnnotation>();
        for (PsiErrorElement syntaxError : AnalyzingUtils.getSyntaxErrorRanges(jetFile)) {
            result.add(createKotlinAnnotation(syntaxError, file));
        }
        
        return result;
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull PsiErrorElement psiErrorElement, @NotNull IFile file) {
        PsiFile psiFile = psiErrorElement.getContainingFile();
        
        TextRange range = psiErrorElement.getTextRange();
        int startOffset = range.getStartOffset();
        int length = range.getLength();
        String markedText = psiErrorElement.getText();
        
        if (range.isEmpty()) {
            startOffset--;
            length = 1;
            markedText = psiFile.getText().substring(startOffset, startOffset + length);
        }
        
        return new DiagnosticAnnotation(
                LineEndUtil.convertLfToDocumentOffset(psiFile.getText(), startOffset, EditorUtil.getDocument(file)),
                length,
                AnnotationManager.ANNOTATION_ERROR_TYPE,
                psiErrorElement.getErrorDescription(),
                markedText,
                null);
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull Diagnostic diagnostic, @NotNull IFile file) {
        TextRange range = diagnostic.getTextRanges().get(0);
        return new DiagnosticAnnotation(
                LineEndUtil.convertLfToDocumentOffset(diagnostic.getPsiFile().getText(), 
                        range.getStartOffset(), EditorUtil.getDocument(file)),
                range.getLength(),
                getAnnotationType(diagnostic.getSeverity()), 
                DefaultErrorMessages.render(diagnostic),
                diagnostic.getPsiElement().getText(),
                diagnostic.getFactory());
    }
    
    @NotNull
    private String getAnnotationType(@NotNull Severity severity) {
        String annotationType = null;
        switch (severity) {
            case ERROR:
                annotationType = AnnotationManager.ANNOTATION_ERROR_TYPE;
                break;
            case WARNING:
                annotationType = AnnotationManager.ANNOTATION_WARNING_TYPE;
                break;
            case INFO:
                throw new UnsupportedOperationException("Diagnostics with severith 'INFO' are not supported");
        }
        
        assert annotationType != null;
        
        return annotationType;
    }
    
    public void updateAnnotations(
            @NotNull AbstractTextEditor editor, 
            @NotNull Map<IFile, List<DiagnosticAnnotation>> annotations,
            @NotNull Predicate<Annotation> replacementAnnotationsPredicate) {
        try {
            List<DiagnosticAnnotation> newAnnotations;
            IFile file = EditorUtil.getFile(editor);
            if (file != null && annotations.containsKey(file)) {
                newAnnotations = annotations.get(file);
                assert newAnnotations != null : "Null element in annotations map for file " + file.getName();
            } else {
                newAnnotations = Collections.emptyList();
            }
            
            AnnotationManager.updateAnnotations(editor, newAnnotations, replacementAnnotationsPredicate);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Nullable
    public DiagnosticAnnotation getAnnotationByOffset(@NotNull AbstractTextEditor editor, int offset) {
        IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        Iterator<?> annotationIterator = annotationModel.getAnnotationIterator();
        while (annotationIterator.hasNext()) {
            Annotation annotation = (Annotation) annotationIterator.next();
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
    public IMarker getMarkerByOffset(@NotNull IFile file, int offset) {
        try {
            IMarker[] markers = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            for (IMarker marker: markers) {
                int startOffset = (int) marker.getAttribute(IMarker.CHAR_START);
                int endOffset = (int) marker.getAttribute(IMarker.CHAR_END);
                if (startOffset <= offset && offset <= endOffset) {
                    return marker;
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    public static boolean isQuickFixable(@NotNull DiagnosticFactory<?> diagnostic) {
        return isUnresolvedReference(diagnostic) || isPublicMemberTypeNotSpecified(diagnostic);
    }

    public static boolean isUnresolvedReference(@NotNull DiagnosticFactory<?> diagnostic) {
        return Errors.UNRESOLVED_REFERENCE.equals(diagnostic);
    }
    
    public static boolean isPublicMemberTypeNotSpecified(@NotNull DiagnosticFactory<?> diagnostic) {
        return Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE.equals(diagnostic);
    }
}