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
package org.jetbrains.kotlin.ui.editors.hover

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.internal.ui.text.java.hover.ProblemHover
import org.eclipse.jface.text.IInformationControlCreator
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.source.Annotation
import org.eclipse.ui.texteditor.MarkerAnnotation
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.annotations.DiagnosticAnnotation
import org.jetbrains.kotlin.ui.editors.quickfix.KotlinMarkerResolutionGenerator
import org.jetbrains.kotlin.ui.editors.quickfix.diagnostic
import org.jetbrains.kotlin.ui.editors.toCompletionProposals

class KotlinAnnotationTextHover : KotlinEditorTextHover<Any> {

    override val hoverPriority: Int
        get() = 1

    private val problemHover = KotlinProblemHover()

    override fun getHoverInfo(hoverData: HoverData): Any? =
        hoverData.getRegion()?.let { region ->
            problemHover.getHoverInfo2(hoverData.editor.javaEditor.viewer, region)
        }

    override fun isAvailable(hoverData: HoverData): Boolean = true

    override fun getHoverControlCreator(editor: KotlinEditor): IInformationControlCreator? =
        problemHover.hoverControlCreator
}

private class KotlinProblemHover : ProblemHover() {

    override fun createAnnotationInfo(
        annotation: Annotation,
        position: Position,
        textViewer: ITextViewer
    ): AnnotationInfo =
        ProblemInfo(annotation, position, textViewer)

    class ProblemInfo(annotation: Annotation, position: Position, textViewer: ITextViewer) :
        ProblemHover.ProblemInfo(annotation, position, textViewer) {

        override fun getCompletionProposals(): Array<ICompletionProposal> = when (annotation) {
            is MarkerAnnotation -> markerAnnotationFixes(annotation)
            is DiagnosticAnnotation -> annotation.diagnostic?.fixes(annotation.file)
            else -> null
        } ?: emptyArray()

        private fun markerAnnotationFixes(annotation: MarkerAnnotation): Array<ICompletionProposal>? =
            with(annotation.marker) { diagnostic?.fixes(resource as IFile) }

        private fun Diagnostic.fixes(file: IFile) =
            KotlinMarkerResolutionGenerator.getResolutions(this).toCompletionProposals(file).toTypedArray()
    }

}