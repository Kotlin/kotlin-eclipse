package org.jetbrains.kotlin.ui.builder

import org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IWorkbenchWindow
import org.eclipse.ui.IWorkbenchWindowActionDelegate
import org.jetbrains.kotlin.preferences.compiler.RebuildJob

class CompileKotlinClassesAction : IWorkbenchWindowActionDelegate {

    override fun run(action: IAction?) {
        RebuildJob { monitor ->
            ResourcesPlugin.getWorkspace().build(INCREMENTAL_BUILD, monitor)
        }.schedule()
    }

    override fun selectionChanged(action: IAction?, selection: ISelection?) {
    }

    override fun init(window: IWorkbenchWindow?) {
    }

    override fun dispose() {
    }
}