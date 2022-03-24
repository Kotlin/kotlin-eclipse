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
 */
package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.internal.ui.text.JavaWordFinder
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.Region
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.ui.texteditor.ITextEditor
import org.jetbrains.kotlin.core.references.createReferences
import org.jetbrains.kotlin.core.utils.getBindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.ui.editors.codeassist.getParentOfType
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction.Companion.OPEN_EDITOR_TEXT

@Suppress("unused")
class KotlinElementHyperlinkDetector : AbstractHyperlinkDetector() {
    override fun detectHyperlinks(
        textViewer: ITextViewer,
        region: IRegion?,
        canShowMultipleHyperlinks: Boolean
    ): Array<IHyperlink>? {
        val textEditor = getAdapter(ITextEditor::class.java)
        if (region == null || textEditor !is KotlinEditor) return null

        val tempProject = textEditor.javaProject ?: return null

        val openAction = textEditor.getAction(OPEN_EDITOR_TEXT) as? KotlinOpenDeclarationAction
            ?: return null

        val tempDocument = textEditor.documentProvider.getDocument(textEditor.editorInput)

        var wordRegion = JavaWordFinder.findWord(tempDocument, region.offset)

        var tempReferenceExpression: KtReferenceExpression? = null
        if (wordRegion == null || wordRegion.length == 0) {
            tempReferenceExpression = EditorUtil.getReferenceExpression(textEditor, region.offset)
            if (tempReferenceExpression is KtOperationReferenceExpression) {
                val tempOffset =
                    LineEndUtil.convertLfOffsetForMixedDocument(tempDocument, tempReferenceExpression.textOffset)
                wordRegion = Region(tempOffset, tempReferenceExpression.textLength)
            } else if (tempReferenceExpression is KtArrayAccessExpression) {
                val tempOffset =
                    LineEndUtil.convertLfOffsetForMixedDocument(tempDocument, tempReferenceExpression.textOffset)
                wordRegion = Region(tempOffset, tempReferenceExpression.textLength)
            } else if (tempReferenceExpression is KtCallExpression) {
                if (KotlinOpenDeclarationAction.getNavigationData(tempReferenceExpression, tempProject) != null) {
                    val tempOffset =
                        LineEndUtil.convertLfOffsetForMixedDocument(tempDocument, tempReferenceExpression.textOffset)
                    wordRegion = Region(tempOffset, tempReferenceExpression.textLength)
                }
            }
        }

        val tempRenderer = DescriptorRenderer.SHORT_NAMES_IN_TYPES.withOptions {
            modifiers = emptySet()
            includeAdditionalModifiers = false
        }

        val tempRef = tempReferenceExpression
            ?: EditorUtil.getReferenceExpression(textEditor, region.offset)
            ?: EditorUtil.getJetElement(textEditor, region.offset)
            ?: return null

        val context = tempRef.getBindingContext()
        val tempTargets = createReferences(tempRef)
            .flatMap { it.getTargetDescriptors(context) }

        return tempTargets.map {
            KTGenericHyperLink(
                wordRegion,
                tempRenderer.render(it),
                textEditor,
                it,
                tempRef
            )
        }.toTypedArray()
    }
}
