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

import org.eclipse.jdt.internal.ui.text.JavaWordFinder
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.Region
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector
import org.eclipse.jface.text.hyperlink.IHyperlink
import org.eclipse.ui.texteditor.ITextEditor
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
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
            }
            else if (tempReferenceExpression is KtCallExpression) {
                if(textEditor.javaProject != null && KotlinOpenDeclarationAction.getNavigationData(tempReferenceExpression, textEditor.javaProject!!) != null) {
                    val tempOffset =
                        LineEndUtil.convertLfOffsetForMixedDocument(tempDocument, tempReferenceExpression.textOffset)
                    wordRegion = Region(tempOffset, tempReferenceExpression.textLength)
                }
            }
        }
        tempReferenceExpression ?: EditorUtil.getReferenceExpression(textEditor, region.offset) ?: return null

        return arrayOf(KotlinElementHyperlink(openAction, wordRegion, tempReferenceExpression))
    }
}