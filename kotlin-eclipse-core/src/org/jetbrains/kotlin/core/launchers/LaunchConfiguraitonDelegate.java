package org.jetbrains.kotlin.core.launchers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

public class LaunchConfiguraitonDelegate extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {    
     
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {
        
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        
        monitor.beginTask("Launching...", 1);

        IVMRunner runner = getVMRunner(configuration, mode);
        //TODO: classToLaunch, class path
        VMRunnerConfiguration vmConfig = new VMRunnerConfiguration("", new String[] {});
        runner.run(vmConfig, launch, monitor);

        monitor.done();
    }
    
}
