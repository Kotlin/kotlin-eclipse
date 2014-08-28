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
package org.jetbrains.kotlin.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.ui.launch.KotlinRuntimeConfigurationSuggestor;

public class NewUnitWizard extends AbstractWizard<NewUnitWizardPage> {
    
    private static final String TITLE_FORMAT = "Kotlin %s File";
    private static final String DESCRIPTION_FORMAT = "Create a new Kotlin %s file";
    
    private static final String DEFAULT_FILE_NAME = "";
    private static final String DEFAULT_PACKAGE_NAME = "";
    private static final String DEFAULT_TYPE_BODY = "";
    private static final String PACKAGE_FORMAT = "package %s\n\n";
    
    protected WizardType type;
    
    public NewUnitWizard() {
        type = WizardType.NONE;
    }
    
    @Override
    public boolean performFinish() {
        NewUnitWizardPage wizardPage = getWizardPage();
        String contents = createPackageHeader() + createTypeBody();
        
        FileCreationOp op = new FileCreationOp(wizardPage.getSourceDir(), wizardPage.getPackageFragment(),
                wizardPage.getUnitName(), contents, getShell());
        performOperation(op);
        
        IProject project = wizardPage.getProject();
        addKotlinNatureToProject(project);
        addKotlinBuilderToProject(project);
        KotlinRuntimeConfigurationSuggestor.suggestForProject(project);
        
        selectAndRevealResource(op.getResult());
        openFile(op.getResult());
        
        return true;
    }
    
    protected void openFile(IFile file) {
        IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
        
        try {
            if (window != null) {
                IWorkbenchPage page = window.getActivePage();
                
                if (page != null) {
                    IDE.openEditor(page, file, true);
                }
            }
        } catch (PartInitException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Override
    protected String getPageTitle() {
        return String.format(TITLE_FORMAT, type.getWizardTypeName());
    }
    
    @Override
    protected NewUnitWizardPage createWizardPage() {
        return new NewUnitWizardPage(getPageTitle(), String.format(DESCRIPTION_FORMAT,
                type.getWizardTypeName().toLowerCase()), DEFAULT_FILE_NAME, getStructuredSelection());
    }
    
    private String createTypeBody() {
        if (type == WizardType.NONE) {
            return DEFAULT_TYPE_BODY;
        }
        
        return String.format(type.getFileBodyFormat(), FileCreationOp.getSimpleUnitName(getWizardPage().getUnitName()));
    }
    
    private String createPackageHeader() {
        String pckg = getWizardPage().getPackageFragment().getElementName();
        if (pckg.isEmpty()) {
            return DEFAULT_PACKAGE_NAME;
        }
        
        return String.format(PACKAGE_FORMAT, pckg);
    }
    
}