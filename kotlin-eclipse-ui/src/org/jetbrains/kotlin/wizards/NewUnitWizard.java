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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.model.KotlinNature;

public class NewUnitWizard extends Wizard implements INewWizard {

    private static final String TITLE_FORMAT = "Kotlin %s File";
    private static final String DESCRIPTION_FORMAT = "Create a new Kotlin %s file";
    private static final String DEFAULT_FILE_NAME = "";
    private static final String PACKAGE_FORMAT = "package %s\n\n";
    
    private IWorkbench workbench;
    private IStructuredSelection selection;
    private NewUnitWizardPage page;
    
    protected WizardType type;
    
    public NewUnitWizard() {
        type = WizardType.NONE;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
        this.setWindowTitle(createPageTitle());
    }

    @Override
    public boolean performFinish() {
        String contents = createPackageHeader() + createTypeBody();
        FileCreationOp op = new FileCreationOp(page.getSourceDir(), page.getPackageFragment(), page.getUnitName(),
                false, contents, getShell());

        try {
            getContainer().run(true, true, op);
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), "Error", e.getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }
        
        try {
            KotlinNature.addNature(page.getProject());
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        try {
            KotlinNature.addBuilder(page.getProject());
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        BasicNewResourceWizard.selectAndReveal(op.getResult(), workbench.getActiveWorkbenchWindow());

        return true;
    }
    
    private String createPageTitle() {
        return String.format(TITLE_FORMAT, type.getWizardTypeName());
    }
    
    private String createTypeBody() {
        if (type == WizardType.NONE) {
            return "";
        }
        
        return String.format(type.getFileBodyFormat(), FileCreationOp.getSimpleUnitName(page.getUnitName()));
    }
    
    private String createPackageHeader() {
        String pckg = page.getPackageFragment().getElementName();
        if (pckg.isEmpty()) {
            return "";
        }
        
        return String.format(PACKAGE_FORMAT, pckg);
    }

    @Override
    public void addPages() {
        super.addPages();

        if (page == null) {
            page = new NewUnitWizardPage(createPageTitle(), String.format(DESCRIPTION_FORMAT, type.getWizardTypeName().toLowerCase()), DEFAULT_FILE_NAME, selection);
        }
        addPage(page);
    }

}
