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

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage;
import org.jetbrains.kotlin.utils.EditorUtil;

import com.intellij.psi.PsiFile;

public class KotlinReconcilingStrategy implements IReconcilingStrategy {

    private final JavaEditor editor;
    
    public KotlinReconcilingStrategy(JavaEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public void setDocument(IDocument document) {
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
    }

    @Override
    public void reconcile(IRegion partition) {
        String sourceCode = EditorUtil.getSourceCode(editor);
        IFile file = EditorUtil.getFile(editor);
        
        KotlinPsiManager.INSTANCE.updatePsiFile(file, sourceCode);
        
        updateLineAnnotations(file);
        updateActiveOutlinePage();
    }
    
    private static void updateLineAnnotations(IFile file) {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        PsiFile psiFile = KotlinPsiManager.INSTANCE.getParsedFile(file);
        
        Diagnostics diagnostics = KotlinAnalyzer.analyzeOnlyOneFileCompletely(javaProject, psiFile).getDiagnostics();        
        Map<IFile, List<DiagnosticAnnotation>> annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics);
        
        List<DiagnosticAnnotation> parsingAnnotations = DiagnosticAnnotationUtil.INSTANCE.createParsingDiagnosticAnnotations(psiFile);
        if (annotations.containsKey(file)) {
            annotations.get(file).addAll(parsingAnnotations);
        } else {
            annotations.put(file, parsingAnnotations);
        }
        
        DiagnosticAnnotationUtil.INSTANCE.updateActiveEditorAnnotations(annotations);
    }
    
    private static void updateActiveOutlinePage() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                
                if (workbenchWindow == null) {
                    return;
                }
                
                AbstractTextEditor editor = (AbstractTextEditor) workbenchWindow.getActivePage().getActiveEditor();
                if (editor != null) {
                    IContentOutlinePage outlinePage = (IContentOutlinePage) editor.getAdapter(IContentOutlinePage.class);
                    if (outlinePage instanceof KotlinOutlinePage) {
                        ((KotlinOutlinePage) outlinePage).refresh();
                    }
                }
            }
        });
    }
}
