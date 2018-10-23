package org.jetbrains.kotlin.preferences.compiler

import org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.preferences.BasePropertyPage
import org.jetbrains.kotlin.preferences.views.compilerPropertiesView
import org.jetbrains.kotlin.swt.builders.asView

class WorkspaceCompilerPropertyPage : BasePropertyPage(), IWorkbenchPreferencePage {

    override val properties = KotlinProperties.workspaceInstance

    override fun init(workbench: IWorkbench?) {
    }

    override fun createUI(parent: Composite): Control =
            parent.asView
                    .compilerPropertiesView(properties) {
                        onIsValidChanged = { setValid(it) }
                    }
                    .control

    override fun afterOk() {
        RebuildJob { monitor ->
            KotlinEnvironment.removeAllEnvironments()
            ResourcesPlugin.getWorkspace().build(FULL_BUILD, monitor)
        }.schedule()
    }

}