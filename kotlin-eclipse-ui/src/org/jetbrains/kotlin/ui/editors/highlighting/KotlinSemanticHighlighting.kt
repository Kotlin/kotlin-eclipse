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
package org.jetbrains.kotlin.ui.editors.highlighting

import org.jetbrains.kotlin.ui.editors.KotlinReconcilingListener
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.jface.preference.IPreferenceStore
import org.jetbrains.kotlin.ui.editors.Configuration.KotlinPresentationReconciler
import org.eclipse.jface.text.ITextPresentationListener
import org.eclipse.jface.text.TextPresentation
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.eclipse.jface.text.IPositionUpdater
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.swt.custom.StyleRange
import org.eclipse.jface.text.Position
import org.eclipse.swt.graphics.TextStyle
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import org.eclipse.swt.graphics.RGB
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.widgets.Display
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.eclipse.swt.SWT

public class KotlinSemanticHighlighter(
        val preferenceStore: IPreferenceStore, 
        val colorManager: IColorManager,
        val presentationReconciler: KotlinPresentationReconciler,
        val editor: KotlinFileEditor) : KotlinReconcilingListener, ITextPresentationListener {
    override fun applyTextPresentation(textPresentation: TextPresentation) {
        val region = textPresentation.getExtent()
        val regionStart = region.getOffset()
        val regionEnd = regionStart + region.getLength()
        
        editor.document.getPositions(getCategory())
            .filter { regionStart <= it.getOffset() && it.getOffset() + it.getLength() <= regionEnd }
            .filterNot { it.isDeleted() }
            .forEach { position ->
                val styleRange = (position as HighlightPosition).createStyleRange()
                textPresentation.replaceStyleRange(styleRange)
            }
    }

    override fun reconcile(file: IFile, editor: KotlinFileEditor) {
        removeAllPositions()
        
        val ktFile = editor.parsedFile
        if (ktFile == null) return
        
        val highlightingVisitor = KotlinSemanticHighlightingVisitor(editor)
        highlightingVisitor.computeHighlightingRanges().forEach { position -> 
            editor.document.addPosition(getCategory(), position)
        }
        
        invalidateTextPresentation()
    }
    
    fun install() {
        val viewer = editor.getViewer()
        if (viewer is JavaSourceViewer) {
            viewer.prependTextPresentationListener(this)
            
            with(editor.document) {
                addPositionCategory(getCategory())
                addPositionUpdater(KotlinPositionUpdater(getCategory()))
            }
        } else {
            KotlinLogger.logWarning("Cannot install Kotlin Semantic highlighter for viewer $viewer")
        }
    }
    
    override fun dispose() {
        super.dispose()
    }
    
    private fun invalidateTextPresentation() {
        val shell = editor.getSite()?.getShell()
        if (shell == null || shell.isDisposed()) return
        
        val display = shell.getDisplay()
        if (display == null || display.isDisposed()) return
        
        display.asyncExec { 
            editor.getViewer().invalidateTextPresentation()
        }
    }
    
    private fun removeAllPositions() {
        editor.document.getPositions(getCategory()).forEach { it.delete() }
    }
    
    private fun HighlightPosition.createStyleRange(): StyleRange {
        val styleRange = StyleRange(findTextStyle(styleAttributes, preferenceStore, colorManager))
        
        styleRange.start = getOffset()
        styleRange.length = getLength()
        
        when {
            styleAttributes.bold && styleAttributes.italic -> styleRange.fontStyle = SWT.BOLD or SWT.ITALIC
            styleAttributes.bold -> styleRange.fontStyle = SWT.BOLD
            styleAttributes.italic -> styleRange.fontStyle = SWT.ITALIC
            else -> styleRange.fontStyle = SWT.NORMAL
        }
        
        return styleRange
    }

    
    private fun getCategory(): String = toString()
    
    private class KotlinPositionUpdater(val category: String): IPositionUpdater {
        override fun update(event: DocumentEvent) {
            val editionOffset = event.getOffset()
            val editionLength = event.getLength()
            val editionEnd = editionOffset + editionLength
            val newText = event.getText()
            
            val newLength = newText?.length ?: 0
            
            for (position in event.getDocument().getPositions(category)) {
                val posOffset = position.getOffset()
                val posEnd = posOffset + position.getLength()
                if (editionOffset <= posEnd) {
                    if (editionEnd < posOffset) {
                        val delta = newLength - editionLength
                        position.setOffset(posOffset + delta)
                    } else {
                        position.delete()
                    }
                }
            }
        }
    }
}

private fun findTextStyle(attributes: KotlinHighlightingAttributes, store: IPreferenceStore, colorManager: IColorManager): TextStyle {
    val style = TextStyle()
    val rgb = getColor(attributes.colorKey, store)
    style.foreground = getColor(rgb, colorManager)
    style.underline = attributes.underline
    
    return style
}

private fun getColor(rgb: RGB?, colorManager: IColorManager): Color? {
    var color: Color? = null
    Display.getDefault().syncExec {
        color = colorManager.getColor(rgb)
    }
    
    return color
}

private fun getColor(key: String, store: IPreferenceStore): RGB {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX
    
    return PreferenceConverter.getColor(store, preferenceKey)
}