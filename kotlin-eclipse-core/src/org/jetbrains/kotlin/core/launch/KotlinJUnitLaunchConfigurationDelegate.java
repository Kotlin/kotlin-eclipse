package org.jetbrains.kotlin.core.launch;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.jetbrains.kotlin.core.compiler.KotlinCompiler.KotlinCompilerResult;
import org.jetbrains.kotlin.core.compiler.KotlinCompilerUtils;

public class KotlinJUnitLaunchConfigurationDelegate extends JUnitLaunchConfigurationDelegate {
    public static final String LAUNCH_CONFIGURATION_TYPE_ID = "org.jetbrains.kotlin.core.launch.jUnitLaunchConfigurationDelegate";
    
    @Override
    public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
            throws CoreException {
        try {
            KotlinCompilerResult compilerResult = KotlinCompilerUtils.compileWholeProject(getJavaProject(configuration));
            if (!compilerResult.compiledCorrectly()) {
                KotlinCompilerUtils.handleCompilerOutput(compilerResult.getCompilerOutput());
                
                abort("Build failed", null, 0);
            }
        } catch (InterruptedException | IOException e) {
            abort("Build error", null, 0);
        }
        
        return super.buildForLaunch(configuration, mode, monitor);
    }
}
