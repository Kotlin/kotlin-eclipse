/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.launch;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.core.Activator;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.compiler.KotlinCompiler;
import org.jetbrains.kotlin.core.compiler.KotlinCompiler.KotlinCompilerResult;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinLaunchDelegate extends JavaLaunchDelegate {
    
    public final static String KT_JDK_ANNOTATIONS_PATH = ProjectUtils.buildLibPath("kotlin-jdk-annotations");
    
    @Override
    public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor)
            throws CoreException {
        List<IFile> projectFiles = KotlinPsiManager.INSTANCE.getFilesByProject(getJavaProjectName(configuration));
        
        try {
            KotlinCompilerResult compilerResult = KotlinCompiler.INSTANCE.compileKotlinFiles(projectFiles, 
                    getJavaProject(configuration), getOutputDir(configuration));
            if (!compilerResult.compiledCorrectly()) {
                IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, "", null);
                IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);
                
                if (handler != null) {
                    handler.handleStatus(status, compilerResult.getCompilerOutput());
                }
                
                abort("Build failed", null, 0);
            }
        } catch (InterruptedException | IOException e) {
            abort("Build error", null, 0);
        }
        
        return super.buildForLaunch(configuration, mode, monitor);
    }
    
    @NotNull
    @Override
    public String getJavaProjectName(ILaunchConfiguration configuration) throws CoreException {
        String result = super.getJavaProjectName(configuration);
        
        if (result != null) {
            return result;
        } else {
            abort("Project name is invalid: " + result, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);
        }
        
        throw new IllegalStateException();
    }
    
    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        String[] oldClasspath = super.getClasspath(configuration);
        String[] newClasspath = new String[oldClasspath.length + 1];
        System.arraycopy(oldClasspath, 0, newClasspath, 0, oldClasspath.length);
        
        newClasspath[oldClasspath.length] = KT_JDK_ANNOTATIONS_PATH;
        
        return newClasspath;
    }
    
    @NotNull
    @Override
    public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        try {
            return getPackageClassName(configuration).toString();
        } catch (IllegalArgumentException e) {
            abort("File with main method not defined", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
        }
        
        throw new IllegalStateException();
    }
    
    @NotNull
    @Override
    public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        String result = super.getMainTypeName(configuration);
        
        if (result != null) {
            return result;
        } else {
            abort("Main type name is null", null, 0);
        }
        
        throw new IllegalStateException();
    }
    
    @NotNull
    private FqName getPackageClassName(ILaunchConfiguration configuration) {
        try {
            FqName mainClassName = new FqName(getMainTypeName(configuration));
            for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(getJavaProjectName(configuration))) {
                if (ProjectUtils.hasMain(file) && ProjectUtils.createPackageClassName(file).equalsTo(mainClassName)) {
                    return mainClassName;
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
        
        throw new IllegalArgumentException();
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