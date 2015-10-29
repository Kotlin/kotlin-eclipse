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
package org.jetbrains.kotlin.ui.launch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor;

public class KotlinLaunchShortcut implements ILaunchShortcut {
    @Override
    public void launch(ISelection selection, String mode) {
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }
        
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        
        List<IFile> files = new ArrayList<IFile>();
        for (Object object : structuredSelection.toList()) {
            if (object instanceof IAdaptable) {
                IResource resource = (IResource) ((IAdaptable) object).getAdapter(IResource.class);
                addFiles(files, resource);
            }
        }
        
        IFile fileWithMain = ProjectUtils.findFilesWithMain(files);
       
        if (fileWithMain == null) {
            launchProject(files.get(0).getProject(), mode);
            return;
        }
        
        launchWithMainClass(fileWithMain, mode);
    }
    
    @Override
    public void launch(IEditorPart editor, String mode) {
        if (editor instanceof KotlinFileEditor) {
            IFile file = EditorUtil.getFile(editor);
            
            if (file == null) {
                KotlinLogger.logError("Failed to retrieve IFile from editor " + editor, null);
                return;
            }
            
            if (ProjectUtils.hasMain(file)) {
                launchWithMainClass(file, mode);
                return;
            }

            launchProject(file.getProject(), mode);
        }
    }
    
    private void launchProject(IProject project, String mode) {
        IFile fileWithMain = ProjectUtils.findFilesWithMain(KotlinPsiManager.INSTANCE.getFilesByProject(project));
        if (fileWithMain != null) {
            launchWithMainClass(fileWithMain, mode);
        }
    }
    
    private void launchWithMainClass(IFile fileWithMain, String mode) {
        ILaunchConfiguration configuration = findLaunchConfiguration(getLaunchConfigurationType(), fileWithMain);
        
        if (configuration == null) {
            configuration = createConfiguration(fileWithMain);
        } 
        
        if (configuration == null) {
            return; 
        } 
        
        DebugUITools.launch(configuration, mode);
    }
    
    public static ILaunchConfiguration createConfiguration(IFile file) {
        ILaunchConfiguration configuration = null;
        ILaunchConfigurationWorkingCopy configWC = null;
        ILaunchConfigurationType configurationType = getLaunchConfigurationType();
        
        try {
            configWC = configurationType.newInstance(null, "Config - " + file.getName());
            configWC.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, ProjectUtils.createPackageClassName(file).toString());
            configWC.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, file.getProject().getName());
            
            configuration = configWC.doSave();
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return configuration;
    }
    
    @Nullable
    private ILaunchConfiguration findLaunchConfiguration(ILaunchConfigurationType configurationType, IFile mainClass) {
        try {
            ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configurationType);
            String mainClassName = ProjectUtils.createPackageClassName(mainClass).toString();
            for (ILaunchConfiguration config : configs) {
                if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String)null).equals(mainClassName) && 
                        config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String)null).equals(mainClass.getProject().getName())) {
                    return config;
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    private void addFiles(List<IFile> files, IResource resource) {
        switch (resource.getType()) {
            case IResource.FILE:
                IFile file = (IFile) resource;
                if (resource.getFileExtension().equals(KotlinFileType.INSTANCE.getDefaultExtension())) {
                    files.add(file);
                }
                
                break;
            case IResource.FOLDER:
            case IResource.PROJECT:
                IContainer container = (IContainer) resource;
                try {
                    for (IResource child : container.members()) {
                        addFiles(files, child);
                    }
                } catch (CoreException e) {
                    KotlinLogger.logAndThrow(e);
                }
                
                break;
        }
    }

    private static ILaunchConfigurationType getLaunchConfigurationType() {
        return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
    }
}