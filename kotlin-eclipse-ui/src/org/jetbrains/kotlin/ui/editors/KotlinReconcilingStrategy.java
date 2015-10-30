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
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache;
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage;

public class KotlinReconcilingStrategy implements IReconcilingStrategy {

    private final KotlinFileEditor editor;
    
    public KotlinReconcilingStrategy(KotlinFileEditor editor) {
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
        SafeRunnable.run(new ISafeRunnable() {
            @Override
            public void run() throws Exception {
                IFile file = EditorUtil.getFile(editor);
                
                if (file != null) {
                    resetCache(file);
                    
                    updateLineAnnotations(file);
                    updateOutlinePage();
                } else {
                    KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
                }
            }
            
            @Override
            public void handleException(Throwable exception) {
                KotlinLogger.logError(exception);
            }
        });
    }
    
    private void resetCache(@NotNull IFile file) {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        KotlinAnalysisProjectCache.INSTANCE.resetCache(javaProject);
        KotlinAnalysisFileCache.INSTANCE.resetCache();
    }
    
    private void updateLineAnnotations(IFile file) {
        IJavaProject javaProject = JavaCore.create(file.getProject());
        KtFile jetFile = KotlinPsiManager.getKotlinFileIfExist(file, EditorUtil.getSourceCode(editor));
        if (jetFile == null) {
            return;
        }
        
        Diagnostics diagnostics = KotlinAnalyzer.analyzeFile(javaProject, jetFile).getAnalysisResult()
                .getBindingContext().getDiagnostics();        
        Map<IFile, List<DiagnosticAnnotation>> annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics);
        
        DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(file, annotations);
        DiagnosticAnnotationUtil.INSTANCE.updateAnnotations(editor, annotations);
    }
    
    private void updateOutlinePage() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IContentOutlinePage outlinePage = (IContentOutlinePage) editor.getAdapter(IContentOutlinePage.class);
                if (outlinePage instanceof KotlinOutlinePage) {
                    ((KotlinOutlinePage) outlinePage).refresh();
                }
            }
        });
    }
}
