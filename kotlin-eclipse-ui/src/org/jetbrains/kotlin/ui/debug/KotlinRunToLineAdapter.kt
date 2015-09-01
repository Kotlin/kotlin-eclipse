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

import org.eclipse.debug.ui.actions.IRunToLineTarget
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.jface.viewers.ISelection
import org.eclipse.debug.core.model.ISuspendResume
import org.eclipse.debug.core.model.IDebugElement
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.jetbrains.kotlin.ui.Activator
import org.eclipse.jdt.debug.ui.IJavaDebugUIConstants
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.debug.core.model.IBreakpoint
import java.util.HashMap
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils
import org.eclipse.jdt.debug.core.JDIDebugModel
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.debug.core.model.IDebugTarget
import org.eclipse.debug.ui.actions.RunToLineHandler
import org.eclipse.core.runtime.NullProgressMonitor

public class KotlinRunToLineAdapter : IRunToLineTarget {
    override fun runToLine(part: IWorkbenchPart, selection: ISelection, target: ISuspendResume) {
        val editor = getEditor(part)
        if (editor == null) throw CoreException(createErrorStatus("Missing editor for $part"))
        
        val file = editor.getFile()
        if (file == null) throw CoreException(createErrorStatus("Missing file for $editor"))
        
        if (target is IAdaptable) {
            val debugTarget = target.getAdapter(javaClass<IDebugTarget>()) as? IDebugTarget
            if (debugTarget == null) return
            
            val lineNumber = (selection as ITextSelection).getStartLine() + 1
            val typeName = findTopmostTypeName(editor.document, lineNumber, file)
            
            if (typeName == null) throw CoreException(createErrorStatus("Line is not valid $lineNumber"))
            
            val breakpoint = createRunToLineBreakpoint(typeName.asString(), lineNumber)
            val handler = RunToLineHandler(debugTarget, target, breakpoint)
            handler.run(NullProgressMonitor())
            return
        }
    }
    
    override fun canRunToLine(part: IWorkbenchPart, selection: ISelection, target: ISuspendResume): Boolean {
        return target is IDebugElement && target.canResume()
    }
    
    private fun createRunToLineBreakpoint(typeName: String, lineNumber: Int): IJavaLineBreakpoint {
        val attributes = HashMap<String, Any>(4)
        BreakpointUtils.addRunToLineAttributes(attributes)
        return JDIDebugModel.createLineBreakpoint(ResourcesPlugin.getWorkspace().getRoot(), typeName, 
                lineNumber, -1, -1, 1, false, attributes)
    }
    
    private fun createErrorStatus(message: String): Status {
        return Status(IStatus.ERROR, Activator.PLUGIN_ID, IJavaDebugUIConstants.INTERNAL_ERROR, message, null)
    }
}

private fun getEditor(part: IWorkbenchPart): KotlinFileEditor? {
    return (part as? KotlinFileEditor) ?: (part.getAdapter(javaClass<KotlinFileEditor>()) as? KotlinFileEditor)
}