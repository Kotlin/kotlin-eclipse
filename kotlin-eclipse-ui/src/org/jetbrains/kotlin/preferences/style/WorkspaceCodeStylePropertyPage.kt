package org.jetbrains.kotlin.preferences.style

import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties
import org.jetbrains.kotlin.preferences.BasePropertyPage
import org.jetbrains.kotlin.preferences.views.codeStylePropertiesView
import org.jetbrains.kotlin.swt.builders.*

class WorkspaceCodeStylePropertyPage : BasePropertyPage(), IWorkbenchPreferencePage {

    override val properties = KotlinCodeStyleProperties.workspaceInstance

    override fun init(workbench: IWorkbench?) {
    }

    override fun createUI(parent: Composite): Control =
            parent.asView.apply {
                codeStylePropertiesView(properties)
            }.control
}
