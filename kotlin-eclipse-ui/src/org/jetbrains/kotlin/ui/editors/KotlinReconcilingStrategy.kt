/*******************************************************************************
* Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.ISafeRunnable
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.reconciler.DirtyRegion
import org.eclipse.jface.text.reconciler.IReconcilingStrategy
import org.eclipse.jface.util.SafeRunnable
import org.eclipse.swt.widgets.Display
import org.eclipse.ui.views.contentoutline.IContentOutlinePage
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage

class KotlinReconcilingStrategy(val editor: KotlinFileEditor) : IReconcilingStrategy {
    override fun setDocument(document: IDocument?) {}
    
    override fun reconcile(dirtyRegion: DirtyRegion?, subRegion: IRegion?) {}
    
    override fun reconcile(partition: IRegion?) {
        SafeRunnable.run(object : ISafeRunnable {
            override fun run() {
                val file = EditorUtil.getFile(editor)
                if (file != null) {
                    resetCache(file)
                    updateLineAnnotations(file)
                    updateOutlinePage()
                } else {
                    KotlinLogger.logError("Failed to retrieve IFile from editor $editor", null)
                }
            }
            
            override fun handleException(exception: Throwable) {
                KotlinLogger.logError(exception)
            }
        })
    }
    
    private fun resetCache(file: IFile) {
        val javaProject = JavaCore.create(file.getProject())
        KotlinAnalysisProjectCache.resetCache(javaProject)
        KotlinAnalysisFileCache.resetCache()
    }
    
    private fun updateLineAnnotations(file: IFile) {
        val javaProject = JavaCore.create(file.getProject())
        val jetFile = KotlinPsiManager.getKotlinFileIfExist(file, EditorUtil.getSourceCode(editor))
        if (jetFile == null) {
            return
        }
        
        val diagnostics = KotlinAnalyzer.analyzeFile(javaProject, jetFile).getAnalysisResult().getBindingContext().getDiagnostics()
        val annotations = DiagnosticAnnotationUtil.INSTANCE.handleDiagnostics(diagnostics)
        DiagnosticAnnotationUtil.INSTANCE.addParsingDiagnosticAnnotations(file, annotations)
        DiagnosticAnnotationUtil.INSTANCE.updateAnnotations(editor, annotations)
    }
    
    private fun updateOutlinePage() {
        Display.getDefault().asyncExec {
            val outlinePage = editor.getAdapter(IContentOutlinePage::class.java) as IContentOutlinePage
            if (outlinePage is KotlinOutlinePage) {
                outlinePage.refresh()
            }
        }
    }
}