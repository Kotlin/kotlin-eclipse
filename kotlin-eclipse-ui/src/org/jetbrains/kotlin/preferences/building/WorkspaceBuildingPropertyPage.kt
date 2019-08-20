package org.jetbrains.kotlin.preferences.building

import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.jetbrains.kotlin.core.preferences.KotlinBuildingProperties
import org.jetbrains.kotlin.preferences.BasePropertyPage
import org.jetbrains.kotlin.preferences.views.buildingPropertiesView
import org.jetbrains.kotlin.swt.builders.asView

class WorkspaceBuildingPropertyPage : BasePropertyPage(), IWorkbenchPreferencePage {

    override val properties = KotlinBuildingProperties.workspaceInstance

    override fun init(workbench: IWorkbench?) {
    }

    override fun createUI(parent: Composite): Control =
        parent.asView
            .buildingPropertiesView(properties) {
                onIsValidChanged = { setValid(it) }
            }
            .control

}