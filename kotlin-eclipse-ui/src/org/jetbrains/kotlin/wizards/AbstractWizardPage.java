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

import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createComposite;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

public abstract class AbstractWizardPage extends WizardPage implements IWizardPage {
    
    protected static final String BROWSE_BUTTON_TITLE = "Browse...";

    protected AbstractWizardPage(String title, String description) {
        super(title);
        super.setTitle(title);
        super.setDescription(description);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = createComposite(parent);
        createControls(composite);
        setControl(composite);

        setPageComplete(false);
    }

    protected abstract void createControls(Composite composite);

    protected abstract String createErrorMessage();

    protected final void validate() {
        String errorMessage = createErrorMessage();

        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
    }

    protected abstract boolean alreadyExists();

    protected static IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

}
