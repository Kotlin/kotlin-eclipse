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
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiUtil
import com.intellij.psi.PsiElement

public class KotlinToggleBreakpointAdapter:IToggleBreakpointsTarget {
    override public fun toggleLineBreakpoints(part:IWorkbenchPart, selection:ISelection) {
        val editor = getEditor(part)
        if (editor == null) return
        
        val file = EditorUtil.getFile(editor)
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null)
            return
        }
        
        val lineNumber = (selection as ITextSelection).getStartLine() + 1
        val document = editor.getDocumentProvider().getDocument(editor.getEditorInput())
        val typeName = getTypeName(document, lineNumber, file)
        
        val existingBreakpoint = JDIDebugModel.lineBreakpointExists(file, typeName, lineNumber)
        if (existingBreakpoint != null) {
            existingBreakpoint.delete()
        } else {
            JDIDebugModel.createLineBreakpoint(file, typeName, lineNumber, -1, -1, 0, true, null)
        }
    }
    
    override public fun canToggleLineBreakpoints(part: IWorkbenchPart, selection: ISelection): Boolean = true
    
    override public fun toggleMethodBreakpoints(part:IWorkbenchPart, selection:ISelection) {}
    
    override public fun canToggleMethodBreakpoints(part: IWorkbenchPart, selection: ISelection): Boolean = true
    
    override public fun toggleWatchpoints(part:IWorkbenchPart, selection:ISelection) {}
    
    override public fun canToggleWatchpoints(part: IWorkbenchPart, selection: ISelection): Boolean = true
    
    private fun getTypeName(document: IDocument, lineNumber: Int, file: IFile): String {
        val kotlinParsedFile = KotlinPsiManager.INSTANCE.getParsedFile(file)
        val typeName = findTopmostType(document.getLineOffset(lineNumber - 1), kotlinParsedFile).asString()
        
        return typeName
    }
    
    private fun findTopmostType(offset: Int, jetFile: JetFile): FqName {
        val element = jetFile.findElementAt(offset)
        val jetClass = JetPsiUtil.getTopmostParentOfTypes(element, javaClass<JetClass>()) as? JetClass
        if (jetClass != null) {
            val fqName = jetClass.getFqName()
            if (fqName != null) { // For example, fqName might be null if jetClass is a local class
                return fqName
            }
        }
        
        return PackageClassUtils.getPackageClassFqName(jetFile.getPackageFqName())
    }
    
    private fun getEditor(part: IWorkbenchPart): ITextEditor? {
        return if (part is ITextEditor) part else part.getAdapter(javaClass<ITextEditor>()) as? ITextEditor
    }
}