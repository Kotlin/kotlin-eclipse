package org.jetbrains.kotlin.core.launchers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.jetbrains.kotlin.core.builder.KotlinManager;

public class LaunchConfiguraitonDelegate extends AbstractJavaLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {    
     
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
            throws CoreException {
        
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        for (IFile file : KotlinManager.getAllProjectFiles()) {
            System.out.println("Compiling file: " + file.getRawLocation().toOSString());
            compileKotlinFile(file, configuration);
        }

        IVMRunner runner = getVMRunner(configuration, mode);
        monitor.beginTask("Launcing", 1);
        
        VMRunnerConfiguration vmConfig = new VMRunnerConfiguration(
                getClassToLaunch(configuration), getClassPath(configuration));
        
        runner.run(vmConfig, launch, monitor);
        
        monitor.done();
    }
    
    private void compileKotlinFile(IFile file, ILaunchConfiguration configuration) {
        StringBuilder sb = new StringBuilder("java");
        sb.append(" -cp ");
        try {
            boolean firsttime = true;
            for (String string : getClasspath(configuration)) {
                if (firsttime) {
                    sb.append(string);
                    firsttime = false;
                } else {
                    sb.append(":" + string);                    
                }
            }
        } catch (CoreException e1) {
            //swallow
        }
        sb.append(" org.jetbrains.jet.cli.jvm.K2JVMCompiler");
        sb.append(" -src " + file.getRawLocation().toOSString());
        sb.append(" -output " + getOutputDir(configuration));
        try {
            System.out.println("Executing: " + sb.toString());
            Runtime.getRuntime().exec(sb.toString()).waitFor();
        } catch (IOException e) {
            //swallow
        } catch (InterruptedException e) {
            //swallow
        }        
    }
    
    private String getOutputDir(ILaunchConfiguration configuration) {
        try {
            String[] cp = getClasspath(configuration);
            if (cp.length > 0)
                return cp[0];
        } catch (CoreException e) {
            //swallow
        }
        
        return ".";
    }
    
    private String getClassToLaunch(ILaunchConfiguration configuration) throws CoreException {
        return verifyMainTypeName(configuration);
    }
    
    private String[] getClassPath(ILaunchConfiguration configuration) throws CoreException {
        List<String> paths = new ArrayList<String>();
        
        for (String string : getClasspath(configuration)) {
            System.out.println("cp: " + string);
            paths.add(string);
        }
                
        String[] result = new String[paths.size()];
        return paths.toArray(result);
    }
    
}
