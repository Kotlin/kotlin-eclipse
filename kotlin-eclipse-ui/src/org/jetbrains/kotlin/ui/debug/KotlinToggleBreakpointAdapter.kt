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
package org.jetbrains.kotlin.ui.debug

import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.CoreException
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint
import org.eclipse.jdt.debug.core.JDIDebugModel
import org.eclipse.jface.text.BadLocationException
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.ui.texteditor.ITextEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider

public class KotlinToggleBreakpointAdapter : IToggleBreakpointsTarget {
    override public fun toggleLineBreakpoints(part: IWorkbenchPart, selection: ISelection) {
        val editor = getEditor(part)
        if (editor == null) return
        
        val file = EditorUtil.getFile(editor)
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return
        }
        
        val lineNumber = (selection as ITextSelection).getStartLine() + 1
        val document = editor.getDocumentProvider().getDocument(editor.getEditorInput())
        val typeName = findTopmostTypeName(document, lineNumber, file)
        if (typeName == null) return
        
        val existingBreakpoint = JDIDebugModel.lineBreakpointExists(file, typeName.asString(), lineNumber)
        if (existingBreakpoint != null) {
            existingBreakpoint.delete()
        } else {
            JDIDebugModel.createLineBreakpoint(file, typeName.asString(), lineNumber, -1, -1, 0, true, null)
        }
    }
    
    override public fun canToggleLineBreakpoints(part: IWorkbenchPart, selection: ISelection): Boolean = true
    
    override public fun toggleMethodBreakpoints(part: IWorkbenchPart, selection: ISelection) {}
    
    override public fun canToggleMethodBreakpoints(part: IWorkbenchPart, selection: ISelection): Boolean = true
    
    override public fun toggleWatchpoints(part: IWorkbenchPart, selection: ISelection) {}
    
    override public fun canToggleWatchpoints(part: IWorkbenchPart, selection: ISelection): Boolean = true
    
    private fun getEditor(part: IWorkbenchPart): ITextEditor? {
        return if (part is ITextEditor) part else part.getAdapter(ITextEditor::class.java) as? ITextEditor
    }
}

fun findTopmostTypeName(document: IDocument, lineNumber: Int, file: IFile): FqName? {
    val kotlinParsedFile = KotlinPsiManager.INSTANCE.getParsedFile(file)
    return findTopmostType(document.getLineOffset(lineNumber - 1), kotlinParsedFile)
}

fun findTopmostType(offset: Int, jetFile: KtFile): FqName? {
    val element = jetFile.findElementAt(offset)
    val jetClassOrObject = KtPsiUtil.getTopmostParentOfTypes(element, KtClassOrObject::class.java)
    if (jetClassOrObject != null) {
        val classOrObject = jetClassOrObject as KtClassOrObject
        val fqName = classOrObject.getFqName()
        if (fqName != null) return fqName
    }
    
    return NoResolveFileClassesProvider.getFileClassInfo(jetFile).fileClassFqName
}