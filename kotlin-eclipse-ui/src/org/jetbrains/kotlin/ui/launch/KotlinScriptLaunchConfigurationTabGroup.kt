package org.jetbrains.kotlin.ui.launch

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup
import org.eclipse.debug.ui.ILaunchConfigurationDialog
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab

class KotlinScriptLaunchConfigurationTabGroup : AbstractLaunchConfigurationTabGroup() {
    override fun createTabs(dialog: ILaunchConfigurationDialog, mode: String) {
        setTabs(arrayOf(JavaArgumentsTab()))
    }
}