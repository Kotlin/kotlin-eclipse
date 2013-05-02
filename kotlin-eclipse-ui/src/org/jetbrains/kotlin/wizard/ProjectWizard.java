package org.jetbrains.kotlin.wizard;


import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.jetbrains.kotlin.model.KotlinProjectSupport;

public class ProjectWizard extends Wizard implements INewWizard, IExecutableExtension  {
    private WizardNewProjectCreationPage projectPropertiesPage;
    private IConfigurationElement configurationElement;
    
    public ProjectWizard() {
        setWindowTitle("New Kotlin Project Wizard");        
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {}

    @Override
    public boolean performFinish() {
        String name = projectPropertiesPage.getProjectName();
        URI location = null;
        if (!projectPropertiesPage.useDefaults()) {
            location = projectPropertiesPage.getLocationURI();
        } 
     
        KotlinProjectSupport.createProject(name, location);

        BasicNewProjectResourceWizard.updatePerspective(configurationElement);
        
        return true;
    }

    @Override
    public void addPages() {
        super.addPages();
        projectPropertiesPage = new WizardNewProjectCreationPage("New Kotlin Project Wizard");
        projectPropertiesPage.setTitle("Create Kotlin Project");
        projectPropertiesPage.setDescription("Create new Kotlin project from scratch.");
     
        addPage(projectPropertiesPage);
    }

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
        configurationElement = config;        
    }
    

}
