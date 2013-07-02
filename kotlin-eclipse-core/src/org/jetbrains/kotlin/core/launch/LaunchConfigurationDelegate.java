package org.jetbrains.kotlin.core.launch;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.core.builder.KotlinManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.osgi.framework.Bundle;

public class LaunchConfigurationDelegate extends JavaLaunchDelegate {

    private final String ktCompiler = "kotlin-compiler-0.5.162.jar";
    private final static String ktHome = getKtHome();
    
    private static String getKtHome() {
        Bundle bundle = Platform.getBundle("org.jetbrains.kotlin.ui");
        Path uiPluginPath = new Path("plugin.xml");
        try {
            Path path = new Path(FileLocator.resolve(FileLocator.find(bundle, uiPluginPath, null)).getPath());
            
            return path.removeLastSegments(2).toPortableString() + "/kotlin-bundled-compiler/";
        } catch (IOException e) {
            KotlinLogger.logError(e);
        }
        
        return null;
    }
    
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        String projectName = getJavaProjectName(configuration);
        
        List<IFile> projectFiles = KotlinManager.getFilesByProject(projectName);
        if (projectFiles == null) {
            abort("Project name is invalid: " + projectName, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);
            
            return;
        }
        
        compileKotlinFiles(projectFiles, configuration);
        
        super.launch(configuration, mode, launch, monitor);
    }
    
    @Override
    public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        try {
            return getPackageClassName(configuration).toString();
        } catch (IllegalArgumentException e) {
            abort("File with main method not defined", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
        }
        
        return null;
    }
    
    private FqName getPackageClassName(ILaunchConfiguration configuration) {
        try {
            String projectName = getJavaProjectName(configuration);
            FqName mainClassName = new FqName(getMainTypeName(configuration));
            for (IFile file : KotlinManager.getFilesByProject(projectName)) {
                if (ProjectUtils.hasMain(file) && ProjectUtils.createPackageClassName(file).equalsTo(mainClassName)) {
                    return mainClassName;
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
        
        throw new IllegalArgumentException();
    }
    
    private void compileKotlinFiles(List<IFile> files, ILaunchConfiguration configuration) throws CoreException {
        StringBuilder command = new StringBuilder();
        command.append("java -cp " + ktHome + "lib/" + ktCompiler);
        command.append(" " + K2JVMCompiler.class.getCanonicalName());
        command.append(" -kotlinHome " + ktHome);


        command.append(" -src ");
        
        IJavaProject javaProject = getJavaProject(configuration);
        IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
        for (IClasspathEntry classpathEntry : classpathEntries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                command.append(javaProject.getProject().getLocation().removeLastSegments(1).toPortableString() + classpathEntry.getPath().toPortableString() + " ");
            }
        }
        
        command.append(" -output " + getOutputDir(configuration));
        
        try {
            Runtime.getRuntime().exec(command.toString()).waitFor();
        } catch (IOException | InterruptedException e) {
            KotlinLogger.logError(e);
            
            abort("Build error", null, 0);
        }
        System.out.println(command);
    }
    
    private String getOutputDir(ILaunchConfiguration configuration) {
        try {
            String[] cp = getClasspath(configuration);
            if (cp.length > 0)
                return cp[0];
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
        
        return ".";
    }
}