package org.jetbrains.kotlin.preferences

import org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.swt.builders.asView

class GlobalCompilerPropertyPage: KotlinCompilerPropertyPage(), IWorkbenchPreferencePage {
    override fun init(workbench: IWorkbench?) {
    }

    override fun createContents(parent: Composite) =
            parent.asView.createOptionsControls().control

    override val kotlinProperties = KotlinProperties.workspaceInstance

    override fun rebuildTask(monitor: IProgressMonitor?) {
        KotlinEnvironment.removeAllEnvironments()
        ResourcesPlugin.getWorkspace().build(FULL_BUILD, monitor)
    }

}