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

import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createButton;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createCheckbox;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createEmptySpace;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createGroup;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createLabel;
import static org.jetbrains.kotlin.wizards.SWTWizardUtils.createText;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

public class NewProjectWizardPage extends AbstractWizardPage {

    private static final String DEFAULT_PROJECT_NAME = "";
    private static final String DEFAULT_PROJECT_LOCATION = "";

    private static final String PROJECT_NAME_LABEL_TITLE = "Project name";
    private static final String LOCATION_GROUP_TITLE = "Location";
    private static final String PROJECT_LOCATION_CHECKBOX_TITLE = "Use default location";
    private static final String PROJECT_LOCATION_LABEL_TITLE = "Project location";

    private static final String EMPTY_PROJECT_NAME_MESSAGE = "Please enter a project name";
    private static final String EMPTY_PROJECT_LOCATION_MESSAGE = "Please enter a project location";
    private static final String PROJECT_EXISTS_MESSAGE = "Project already exists";

    private String projectName;
    private String projectLocation;

    protected NewProjectWizardPage(String title, String description) {
        super(title, description);

        projectName = DEFAULT_PROJECT_NAME;
        projectLocation = getOSWorkspaceLocation();
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectLocation() {
        return projectLocation;
    }

    @Override
    protected void createControls(Composite parent) {
        Text projectName = createNameField(parent);
        projectName.forceFocus();       

        Group group = createGroup(parent, LOCATION_GROUP_TITLE);

        final Button checkbox = createCheckbox(group, PROJECT_LOCATION_CHECKBOX_TITLE);
        checkbox.setSelection(true);

        final Text locationField = createLocationField(group);
        locationField.setEnabled(false);

        checkbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean selection = checkbox.getSelection();

                locationField.setText(selection ? DEFAULT_PROJECT_LOCATION : projectLocation);
                if (selection) {
                    projectLocation = getOSWorkspaceLocation();
                }

                locationField.setEnabled(!selection);
            }
        });
    }

    private Text createNameField(Composite parent) {
        createLabel(parent, PROJECT_NAME_LABEL_TITLE);

        final Text nameText = createText(parent, DEFAULT_PROJECT_NAME);
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                projectName = nameText.getText();
                validate();
            }
        });

        createEmptySpace(parent);

        return nameText;
    }

    private Text createLocationField(Composite parent) {
        createLabel(parent, PROJECT_LOCATION_LABEL_TITLE);

        final Text locationText = createText(parent, DEFAULT_PROJECT_LOCATION);
        locationText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                projectLocation = locationText.getText();
                validate();
            }
        });

        createButton(parent, BROWSE_BUTTON_TITLE, new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.OPEN);
                dd.setFilterPath(locationText.getText());

                String directoryPath = dd.open();
                if (directoryPath != null) {
                    projectLocation = directoryPath;
                    locationText.setText(directoryPath);
                }

                validate();
            }
        });

        return locationText;
    }

    @Override
    protected String createErrorMessage() {
        if (projectName.isEmpty()) {
            return EMPTY_PROJECT_NAME_MESSAGE;
        } else if (projectLocation.isEmpty()) {
            return EMPTY_PROJECT_LOCATION_MESSAGE;
        } else if (resourceAlreadyExists()) {
            return PROJECT_EXISTS_MESSAGE;
        } else {
            return null;
        }
    }

    @Override
    protected boolean resourceAlreadyExists() {
        for (IProject project : getWorkspaceRoot().getProjects()) {
            if (project.getName().equals(projectName)) {
                return true;
            }
        }

        return false;
    }

    private static String getOSWorkspaceLocation() {
        return getWorkspaceRoot().getLocation().toOSString();
    }

}
