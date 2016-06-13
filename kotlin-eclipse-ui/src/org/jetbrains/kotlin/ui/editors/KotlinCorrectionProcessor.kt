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

import org.eclipse.core.resources.IFile
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor
import org.eclipse.jface.text.source.Annotation
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Point
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.texteditor.MarkerAnnotation
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.eclipse.ui.utils.getBindingContext
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProcessor
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolution
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionGenerator

class KotlinCorrectionProcessor(val editor: KotlinEditor) : IQuickAssistProcessor {
    
    override fun getErrorMessage(): String? = null
    
    override fun canFix(annotation: Annotation): Boolean {
        return annotation is MarkerAnnotation && IDE.getMarkerHelpRegistry().hasResolutions(annotation.marker)
    }
    
    override fun canAssist(invocationContext: IQuickAssistInvocationContext): Boolean = true
    
    override fun computeQuickAssistProposals(invocationContext: IQuickAssistInvocationContext): Array<ICompletionProposal> {
        val diagnostics = findDiagnosticsBy(invocationContext, editor)
        val quickFixResolutions = KotlinMarkerResolutionGenerator.getResolutions(diagnostics)
        
        return arrayListOf<ICompletionProposal>().apply { 
            val file = editor.eclipseFile
            if (file != null) {
                addAll(quickFixResolutions.map { KotlinMarkerResolutionProposal(file, it) })
            }
            
            addAll(KotlinQuickAssistProcessor.getAssists(null, null))
        }.toTypedArray()
    }
}

private class KotlinMarkerResolutionProposal(
        private val file: IFile,
        private val resolution: KotlinMarkerResolution) : ICompletionProposal {
    override fun getImage(): Image? = resolution.image
    
    override fun getAdditionalProposalInfo(): String? = resolution.description
    
    override fun apply(document: IDocument?) {
        resolution.apply(file)
    }
    
    override fun getContextInformation(): IContextInformation? = null
    
    override fun getDisplayString(): String? = resolution.label
    
    override fun getSelection(document: IDocument?): Point? = null
}

fun findDiagnosticsBy(invocationContext: IQuickAssistInvocationContext, editor: KotlinEditor): List<Diagnostic> {
    val offset = LineEndUtil.convertCrToDocumentOffset(editor.document, invocationContext.offset)
    val ktFile = editor.parsedFile ?: return emptyList()
    val javaProject = editor.javaProject ?: return emptyList()
    
    val diagnostics = getBindingContext(ktFile, javaProject)?.diagnostics ?: return emptyList()
    return diagnostics.filter { 
       val range = it.psiElement.textRange
       range.startOffset <= offset && offset <= range.endOffset
    }
}