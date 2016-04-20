/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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

import java.util.ArrayList
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.runtime.CoreException
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.ui.IMarkerResolution
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.texteditor.AbstractTextEditor
import org.eclipse.ui.texteditor.IDocumentProvider
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotationUtil
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProcessor
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionProposal
import com.google.common.collect.Lists
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal

class KotlinCorrectionProcessor(val editor: AbstractTextEditor) : IQuickAssistProcessor {
    
    override fun getErrorMessage(): String? = null
    
    override fun canFix(annotation: Annotation): Boolean {
        val documentProvider = editor.getDocumentProvider()
        val annotationModel = documentProvider.getAnnotationModel(editor.getEditorInput())
        
        val file = EditorUtil.getFile(editor)
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return false
        }
        
        val position = annotationModel.getPosition(annotation)
        val marker = DiagnosticAnnotationUtil.INSTANCE.getMarkerByOffset(file, position.getOffset())
        return IDE.getMarkerHelpRegistry().hasResolutions(marker)
    }
    
    override fun canAssist(invocationContext: IQuickAssistInvocationContext): Boolean = true
    
    override fun computeQuickAssistProposals(invocationContext: IQuickAssistInvocationContext): Array<ICompletionProposal> {
        val completionProposals = ArrayList<ICompletionProposal>()
        
        val marker = EditorUtil.getFile(editor)?.let { 
            val caretOffset = invocationContext.getOffset()
            DiagnosticAnnotationUtil.INSTANCE.getMarkerByOffset(it, caretOffset)
        }
        
        if (marker != null) {
            IDE.getMarkerHelpRegistry().getResolutions(marker).mapTo(completionProposals) { 
                KotlinMarkerResolutionProposal(marker, it) 
            }
        }
        
        completionProposals.addAll(KotlinQuickAssistProcessor.getAssists(null, null))
        
        return completionProposals.toTypedArray()
    }
}