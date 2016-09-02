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

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jdt.internal.ui.text.JavaWordFinder
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover
import org.eclipse.jface.text.IInformationControlCreator
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextHover
import org.eclipse.jface.text.ITextHoverExtension
import org.eclipse.jface.text.ITextHoverExtension2
import org.eclipse.jface.text.ITextViewer
import org.jetbrains.kotlin.core.model.loadExecutableEP
import org.jetbrains.kotlin.eclipse.ui.utils.findElementByDocumentOffset
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.ui.editors.KotlinEditor

const val TEXT_HOVER_EP_ID = "org.jetbrains.kotlin.ui.editor.textHover"

class KotlinTextHover(private val editor: KotlinEditor) : ITextHover, ITextHoverExtension, ITextHoverExtension2 {
    val extensionsHovers = loadExecutableEP<KotlinEditorTextHover>(TEXT_HOVER_EP_ID).mapNotNull { it.createProvider() }
    
    private var hoverData: HoverData? = null
    private var bestHover: KotlinEditorTextHover? = null
    
    override fun getHoverRegion(textViewer: ITextViewer, offset: Int): IRegion? {
        return JavaWordFinder.findWord(textViewer.getDocument(), offset)
    }

    override fun getHoverInfo(textViewer: ITextViewer?, hoverRegion: IRegion): String? {
        return getHoverInfo2(textViewer, hoverRegion)?.toString()
    }

    override fun getHoverControlCreator(): IInformationControlCreator? {
        return bestHover?.getHoverControlCreator(editor)
    }

    override fun getHoverInfo2(textViewer: ITextViewer?, hoverRegion: IRegion): Any? {
        val data: HoverData = createHoverData(hoverRegion.offset) ?: return null

        bestHover = null
        var hoverInfo: Any? = null
        
        for (hover in extensionsHovers) {
            if (hover.isAvailable(data)) {
                hoverInfo = hover.getHoverInfo(data)
                if (hoverInfo != null) {
                    bestHover = hover
                    break
                }
            }
        }
        
        return hoverInfo
    }
    
    private fun createHoverData(offset: Int): HoverData? {
        val ktFile = editor.parsedFile ?: return null
        val psiElement = ktFile.findElementByDocumentOffset(offset, editor.document) ?: return null
        val ktElement = PsiTreeUtil.getParentOfType(psiElement, KtElement::class.java) ?: return null
        
        return HoverData(ktElement, editor)
    }
}

abstract class KotlinEditorTextHover {
    abstract fun getHoverInfo(hoverData: HoverData): Any?
    
    abstract fun isAvailable(hoverData: HoverData): Boolean
    
    open fun getHoverControlCreator(editor: KotlinEditor): IInformationControlCreator? {
        return JavadocHover.PresenterControlCreator(editor.javaEditor.getSite())
    }
}

data class HoverData(val hoverElement: KtElement, val editor: KotlinEditor)