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
package org.jetbrains.kotlin.ui.editors.codeassist

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.TextPresentation
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.jface.text.contentassist.IContextInformationPresenter
import org.eclipse.jface.text.contentassist.IContextInformationValidator
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import kotlin.properties.Delegates

class KotlinParameterListValidator(val editor: KotlinEditor) : IContextInformationValidator,
    IContextInformationPresenter {
    var info: KotlinFunctionParameterContextInformation by Delegates.notNull()
    var viewer: ITextViewer by Delegates.notNull()
    var position: Int by Delegates.notNull()
    var previousIndex: Int by Delegates.notNull()

    override fun install(info: IContextInformation, viewer: ITextViewer, offset: Int) {
        this.info = info as KotlinFunctionParameterContextInformation
        this.viewer = viewer
        this.position = offset
        this.previousIndex = -1
    }

    override fun isContextInformationValid(offset: Int): Boolean {
        EditorUtil.updatePsiFile(editor)

        val document = viewer.document
        val line = document.getLineInformationOfOffset(position)

        if (offset < line.offset) return false

        val currentArgumentIndex = getCurrentArgumentIndex(offset)
        if (currentArgumentIndex == null || isIndexOutOfBound(currentArgumentIndex)) {
            return false
        }

        val expression = getCallSimpleNameExpression(editor, offset)

        return expression?.getReferencedName() == info.name.asString()
    }

    override fun updatePresentation(offset: Int, presentation: TextPresentation): Boolean {
        val currentArgumentIndex = getCurrentArgumentIndex(offset)
        if (currentArgumentIndex == null || previousIndex == currentArgumentIndex) {
            return false
        }
        presentation.clear()
        previousIndex = currentArgumentIndex

        if (isIndexOutOfBound(currentArgumentIndex)) return false

        val renderedParameter = info.renderedParameters[currentArgumentIndex]

        val displayString = info.informationDisplayString
        val start = displayString.indexOf(renderedParameter)
        if (start >= 0) {
            presentation.addStyleRange(StyleRange(0, start, null, null, SWT.NORMAL))

            val end = start + renderedParameter.length
            presentation.addStyleRange(StyleRange(start, end - start, null, null, SWT.BOLD))
            presentation.addStyleRange(StyleRange(end, displayString.length - end, null, null, SWT.NORMAL))

            return true
        }

        return true
    }

    private fun isIndexOutOfBound(index: Int): Boolean = info.renderedParameters.size <= index

    //    Copied with some changes from JetFunctionParameterInfoHandler.java
    private fun getCurrentArgumentIndex(offset: Int): Int? {
        val psiElement = EditorUtil.getPsiElement(editor, offset)
        val argumentList =
            PsiTreeUtil.getNonStrictParentOfType(psiElement, KtValueArgumentList::class.java) ?: return null

        val offsetInPSI = LineEndUtil.convertCrToDocumentOffset(editor.document, offset)
        var child = argumentList.node.firstChildNode
        var index = 0
        while (child != null && child.startOffset < offsetInPSI) {
            if (child.elementType == KtTokens.COMMA ||
                (child.text == "," && child is PsiErrorElement)
            ) ++index
            child = child.treeNext
        }

        return index
    }
}
