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

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.actions.SelectionDispatchAction
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.ui.formatter.EclipseDocumentRange
import org.jetbrains.kotlin.ui.formatter.createPsiFactory
import org.jetbrains.kotlin.ui.formatter.formatRange

class KotlinFormatAction(private val editor: KotlinEditor) : SelectionDispatchAction(editor.javaEditor.site) {
    companion object {
        @JvmField val FORMAT_ACTION_TEXT: String = "Format"
    }
    
    init {
        setText(FORMAT_ACTION_TEXT)
        setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT)
    }
    
    override fun run(selection: ITextSelection) {
        val file = editor.eclipseFile
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return
        }
        
        formatRange(editor.document, getRange(selection), createPsiFactory(file), file.name)
        
        KotlinPsiManager.commitFile(file, editor.document)
    }
    
    private fun getRange(selection: ITextSelection): EclipseDocumentRange {
        val selectionLength = selection.length
        return if (selectionLength == 0) {
            EclipseDocumentRange(0, editor.document.length)
        } else {
            EclipseDocumentRange(selection.offset, selection.offset + selectionLength)
        }
    }
}