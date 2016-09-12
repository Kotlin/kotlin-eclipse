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
import org.jetbrains.kotlin.ui.editors.highlighting.HighlightPosition.StyleAttributes
import org.jetbrains.kotlin.ui.editors.highlighting.HighlightPosition.SmartCast
import org.eclipse.jface.text.source.Annotation
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager
import org.jetbrains.kotlin.ui.editors.annotations.withLock
import org.eclipse.jface.text.source.IAnnotationModelExtension
import org.jetbrains.kotlin.ui.editors.KotlinReconcilingStrategy
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.jface.text.ITextInputListener
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.eclipse.ui.utils.runJob
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.Status
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.KotlinScriptEditor
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor

private val SMART_CAST_ANNOTATION_TYPE = "org.jetbrains.kotlin.ui.annotation.smartCast"

public class KotlinSemanticHighlighter(
        val preferenceStore: IPreferenceStore, 
        val colorManager: IColorManager,
        val presentationReconciler: KotlinPresentationReconciler,
        val editor: KotlinEditor) : KotlinReconcilingListener, ITextPresentationListener, IPropertyChangeListener, ITextInputListener {
    private val positionUpdater by lazy { KotlinPositionUpdater(category) }
    
    private val category by lazy { toString() }
    
    override fun applyTextPresentation(textPresentation: TextPresentation) {
        if (!editor.document.containsPositionCategory(category)) {
            KotlinLogger.logWarning("There is no position category for editor")
            return
        }
        
        val region = textPresentation.getExtent()
        val regionStart = region.getOffset()
        val regionEnd = regionStart + region.getLength()
        
        editor.document.getPositions(category)
            .filter { regionStart <= it.getOffset() && it.getOffset() + it.getLength() <= regionEnd }
            .filterNot { it.isDeleted() }
            .forEach { position ->
                when (position) {
                    is StyleAttributes -> {
                        val styleRange = position.createStyleRange()
                        
                        textPresentation.replaceStyleRange(styleRange)
                    }
                }
            }
    }

    override fun reconcile(file: IFile, editor: KotlinEditor) {
        val document = when (editor) {
            is KotlinCommonEditor -> editor.getDocumentSafely()
            else -> null
        }
        
        if (document == null) return
        
        removeAllPositions(document)
        
        val ktFile = editor.parsedFile
        if (ktFile == null) return
        
        val highlightingVisitor = KotlinSemanticHighlightingVisitor(ktFile, editor.document)
        val smartCasts = arrayListOf<SmartCast>()
        highlightingVisitor.computeHighlightingRanges().forEach { position -> 
            when (position) {
                is StyleAttributes -> editor.document.addPosition(category, position)
                is SmartCast -> smartCasts.add(position)
            }
            
        }
        
        invalidateTextPresentation()
        setupSmartCastsAsAnnotations(smartCasts)
    }
    
    override fun propertyChange(event: PropertyChangeEvent) {
        if (event.getProperty().startsWith(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX)) {
            editor.eclipseFile?.let { reconcile(it, editor) }
        }
    }
    
    override fun inputDocumentChanged(oldInput: IDocument?, newInput: IDocument?) {
        if (newInput != null) {
            manageDocument(newInput)
            val file = editor.eclipseFile
            if (file != null) reconcile(file, editor)
        }
    }
    
    override fun inputDocumentAboutToBeChanged(oldInput: IDocument?, newInput: IDocument?) {
        if (oldInput != null) {
            removeAllPositions(oldInput)
            releaseDocument(oldInput)
        }
    }
    
    fun install() {
        val viewer = editor.javaEditor.getViewer()
        val file = editor.eclipseFile
        if (file != null && viewer is JavaSourceViewer) {
            manageDocument(editor.document)
            
            preferenceStore.addPropertyChangeListener(this)
            
            viewer.addTextInputListener(this)
            viewer.prependTextPresentationListener(this)
            
            runJob("Install semantic highlighting", Job.DECORATE) {
                reconcile(file, editor)
                Status.OK_STATUS
            }
        } else {
            KotlinLogger.logWarning("Cannot install Kotlin Semantic highlighter for viewer $viewer")
        }
    }
    
    fun uninstall() {
        val viewer = editor.javaEditor.getViewer()
        if (viewer is JavaSourceViewer) {
            viewer.removeTextPresentationListener(this)
            viewer.removeTextInputListener(this)
            
            releaseDocument(editor.document)
            
            preferenceStore.removePropertyChangeListener(this)
        }
    }
    
    private fun manageDocument(document: IDocument) {
        document.addPositionCategory(category)
        document.addPositionUpdater(positionUpdater)
    }
    
    private fun releaseDocument(document: IDocument) {
        if (document.containsPositionCategory(category)) {
            document.removePositionCategory(category)            
        }
        document.removePositionUpdater(positionUpdater)
    }
    
    private fun setupSmartCastsAsAnnotations(positions: List<SmartCast>) {
        val annotationMap = positions.associateBy { 
            Annotation(SMART_CAST_ANNOTATION_TYPE, false, "Smart cast to ${it.typeName}")
        }
        
        AnnotationManager.updateAnnotations(editor, annotationMap, SMART_CAST_ANNOTATION_TYPE)
    }
    
    private fun invalidateTextPresentation() {
        val shell = editor.javaEditor.getSite()?.getShell()
        if (shell == null || shell.isDisposed()) return
        
        val display = shell.getDisplay()
        if (display == null || display.isDisposed()) return
        
        display.asyncExec {
            editor.javaEditor.getViewer()?.invalidateTextPresentation()
        }
    }
    
    private fun removeAllPositions(document: IDocument) {
        document.getPositions(category).forEach { it.delete() }
    }
    
    private fun StyleAttributes.createStyleRange(): StyleRange {
        val styleKey = styleAttributes.styleKey
        if (!isEnabled(styleKey, preferenceStore)) {
            return createStyleRange(getOffset(), getLength())
        }
        
        val textStyle = findTextStyle(styleAttributes, preferenceStore, colorManager)
        return with(StyleRange(textStyle)) {
            start = getOffset()
            length = getLength()
            
            fontStyle = SWT.NORMAL
            if (isBold(styleKey, preferenceStore)) fontStyle = fontStyle or SWT.BOLD
            if (isItalic(styleAttributes.styleKey, preferenceStore)) fontStyle = fontStyle or SWT.ITALIC
            
            this
        }
    }
}

private fun findTextStyle(attributes: KotlinHighlightingAttributes, store: IPreferenceStore, colorManager: IColorManager): TextStyle {
    val style = TextStyle()
    val rgb = getColor(attributes.styleKey, store)
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

private fun isBold(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX
    
    return store.getBoolean(preferenceKey)
}

private fun isItalic(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX
    
    return store.getBoolean(preferenceKey)
}

private fun isEnabled(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX
    
    return store.getBoolean(preferenceKey)
}

private fun createStyleRange(s: Int, l: Int) = with(StyleRange()) {
    start = s
    length = l
    
    this
}