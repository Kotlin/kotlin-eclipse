package org.jetbrains.kotlin.ui.launchers;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

public class LaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    public LaunchConfigurationTabGroup() {
    }

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                new LaunchConfigurationTabMain()
        };
        setTabs(tabs);
    }

}
