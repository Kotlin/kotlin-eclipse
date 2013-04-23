package org.jetbrains.kotlin.ui.launchers;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class LaunchConfigurationTabMain implements ILaunchConfigurationTab {

    @Override
    public void createControl(Composite parent) {
    }

    @Override
    public Control getControl() {
        return null;
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public String getMessage() {
        return null;
    }

    @Override
    public boolean isValid(ILaunchConfiguration launchConfig) {
        return true;
    }

    @Override
    public boolean canSave() {
        return false;
    }

    @Override
    public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
    }

    @Override
    public void launched(ILaunch launch) {
        System.out.println("Launched");
    }

    @Override
    public String getName() {
        return "Main";
    }

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
        System.out.println("Activated");
    }

    @Override
    public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
    }

}
