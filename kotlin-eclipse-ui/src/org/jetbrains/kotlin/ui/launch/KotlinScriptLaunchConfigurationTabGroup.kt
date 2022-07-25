package org.jetbrains.kotlin.ui.launch

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup
import org.eclipse.debug.ui.ILaunchConfigurationDialog
import org.eclipse.debug.ui.CommonTab
import org.eclipse.debug.ui.ILaunchConfigurationTab
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab

class KotlinScriptLaunchConfigurationTabGroup : AbstractLaunchConfigurationTabGroup() {
    override fun createTabs(dialog: ILaunchConfigurationDialog, mode: String) {
        val arr = arrayOf(CommonTab())
        setTabs(*arr)
    }
}