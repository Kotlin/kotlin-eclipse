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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

public class KotlinLaunchShortcut implements ILaunchShortcut {

    private final String launchConfigurationTypeId = "org.jetbrains.kotlin.core.launch.launchConfigurationType";
    
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
        
        IFile mainClass = ProjectUtils.getMainClass(files);
        
        if (mainClass == null) {
            launchProject(files.get(0).getProject(), mode);
            
            return;
        }
        
        launchWithMainClass(mainClass, mode);
    }
    
    @Override
    public void launch(IEditorPart editor, String mode) {
        if (editor instanceof KotlinEditor) {
            IEditorInput editorInput = editor.getEditorInput();
            if (!(editorInput instanceof IFileEditorInput)) {
                return;
            } 
            
            IFile file = ((IFileEditorInput) editorInput).getFile();
            
            if (ProjectUtils.hasMain(file)) {
                launchWithMainClass(file, mode);
                
                return;
            }

            launchProject(file.getProject(), mode);
        }
    }
    
    private void launchProject(IProject project, String mode) {
        IFile mainClass = ProjectUtils.getMainClass(KotlinPsiManager.INSTANCE.getFilesByProject(project));
        if (mainClass != null) {
            launchWithMainClass(mainClass, mode);
        }
    }
    
    private void launchWithMainClass(IFile mainClass, String mode) {
        ILaunchConfiguration configuration = findLaunchConfiguration(getLaunchConfigurationType(), mainClass);
        
        if (configuration == null) {
            configuration = createConfiguration(mainClass);
        } 
        
        if (configuration == null) {
            return; 
        } 
        
        DebugUITools.launch(configuration, mode);
    }
    
    private ILaunchConfiguration createConfiguration(IFile file) {
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
                if (resource.getFileExtension().equals(JetFileType.INSTANCE.getDefaultExtension())) {
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

    private ILaunchConfigurationType getLaunchConfigurationType() {
        return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(launchConfigurationTypeId);
    }
}