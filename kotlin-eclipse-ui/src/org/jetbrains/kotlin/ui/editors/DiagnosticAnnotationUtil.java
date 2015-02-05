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
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
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
            
            DiagnosticAnnotation annotation = createKotlinAnnotation(diagnostic);
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
        return recursiveCreateParsingDiagnosticAnnotations(KotlinPsiManager.INSTANCE.getParsedFile(file));
    }
    
    @NotNull
    private List<DiagnosticAnnotation> recursiveCreateParsingDiagnosticAnnotations(@NotNull PsiElement psiElement) {
        List<DiagnosticAnnotation> result = new ArrayList<DiagnosticAnnotation>();
        
        if (psiElement instanceof PsiErrorElement) {
            result.add(createKotlinAnnotation((PsiErrorElement) psiElement));
        } else {
            for (PsiElement child : psiElement.getChildren()) {
                result.addAll(recursiveCreateParsingDiagnosticAnnotations(child));
            }
        }

        return result;
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull PsiErrorElement psiErrorElement) {
        PsiFile psiFile = psiErrorElement.getContainingFile();
        
        TextRange range = psiErrorElement.getTextRange();
        int startOffset = range.getStartOffset();
        int length = range.getLength();
        String markedText = psiErrorElement.getText();
        
        if (range.isEmpty()) {
            startOffset--;
            length++;
            markedText = psiFile.getText().substring(startOffset, startOffset + length);
        }
        
        startOffset = getOffset(psiFile, range.getStartOffset());
        if (range.isEmpty()) {
            startOffset--;
        }
        
        return new DiagnosticAnnotation(
                startOffset,
                length,
                AnnotationManager.ANNOTATION_ERROR_TYPE,
                psiErrorElement.getErrorDescription(),
                markedText,
                false);
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull Diagnostic diagnostic) {
        TextRange range = diagnostic.getTextRanges().get(0);

        return new DiagnosticAnnotation(
                getOffset(diagnostic.getPsiFile(), range.getStartOffset()),
                range.getLength(),
                getAnnotationType(diagnostic.getSeverity()), 
                DefaultErrorMessages.render(diagnostic),
                diagnostic.getPsiElement().getText(),
                Errors.UNRESOLVED_REFERENCE.equals(diagnostic.getFactory()));
    }
    
    private int getOffset(@NotNull PsiFile psiFile, int startOffset) {
        return LineEndUtil.convertLfToOsOffset(psiFile.getText(), startOffset);
    }
    
    @Nullable
    private String getAnnotationType(@NotNull Severity severity) {
        String annotationType = null;
        switch (severity) {
            case ERROR:
                annotationType = AnnotationManager.ANNOTATION_ERROR_TYPE;
                break;
            case WARNING:
                annotationType = AnnotationManager.ANNOTATION_WARNING_TYPE;
                break;
            default:
                break;
        }
        
        return annotationType;
    }
    
    public void updateAnnotations(@NotNull AbstractTextEditor editor, 
            @NotNull Map<IFile, List<DiagnosticAnnotation>> annotations) {
        IFile file = EditorUtil.getFile(editor);

        List<DiagnosticAnnotation> newAnnotations = annotations.get(file);
        if (newAnnotations == null) {
            newAnnotations = Collections.emptyList();
        }
        AnnotationManager.updateAnnotations(editor, newAnnotations);
    }
}