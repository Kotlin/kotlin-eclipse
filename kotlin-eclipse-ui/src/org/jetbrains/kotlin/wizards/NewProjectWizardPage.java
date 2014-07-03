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

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewProjectWizardPage extends WizardPage implements IWizardPage {

    private static final String DEFAULT_PROJECT_NAME = "untitled";
    private static final String PROJECT_NAME_TITLE = "Project name: ";
    private static final String PROJECT_LOCATION_TITLE = "Project location: ";
    private static final String SELECT_LOCATION_BUTTON_TITLE = "...";
    private static final String EMPTY_PROJECT_NAME_MESSAGE = "Please enter a project name";
    private static final String EMPTY_PROJECT_LOCATION_MESSAGE = "Please enter a project location";
    private static final String PROJECT_EXISTS_MESSAGE = "Project already exists";

    private String projectName;
    private String projectLocation;

    protected NewProjectWizardPage(String title, String description) {
        super(title);
        super.setTitle(title);
        super.setDescription(description);

        projectName = DEFAULT_PROJECT_NAME;
        projectLocation = getOSWorkspaceLocation();
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());

        GridLayout layout = new GridLayout();
        layout.numColumns = 4;
        composite.setLayout(layout);

        createControls(composite);
        setControl(composite);

        setFormErrorMessage();
    }

    private void createControls(Composite composite) {
        Text projectName = createNameField(composite);
        projectName.setText(this.projectName);
        projectName.forceFocus();

        Text projectLocation = createLocationField(composite);
        projectLocation.setText(this.projectLocation);
    }

    private Text createNameField(Composite composite) {
        GridData lgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        lgd.horizontalSpan = 1;

        Label nameLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
        nameLabel.setText(PROJECT_NAME_TITLE);
        nameLabel.setLayoutData(lgd);

        GridData tgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        tgd.horizontalSpan = 2;
        tgd.grabExcessHorizontalSpace = true;

        final Text nameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        nameText.setLayoutData(tgd);
        nameText.setText(projectName);
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                projectName = nameText.getText();
                setFormErrorMessage();
            }
        });

        new Label(composite, SWT.NONE);

        return nameText;
    }

    private Text createLocationField(Composite composite) {
        GridData lgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        lgd.horizontalSpan = 1;

        Label locationLabel = new Label(composite, SWT.LEFT | SWT.WRAP);
        locationLabel.setText(PROJECT_LOCATION_TITLE);
        locationLabel.setLayoutData(lgd);

        GridData tgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        tgd.horizontalSpan = 2;
        tgd.grabExcessHorizontalSpace = true;

        final Text locationText = new Text(composite, SWT.SINGLE | SWT.BORDER);
        locationText.setLayoutData(tgd);

        locationText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                projectLocation = locationText.getText();
                setFormErrorMessage();
            }
        });

        GridData bgd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        bgd.horizontalSpan = 1;

        Button selectLocationButton = new Button(composite, SWT.PUSH);
        selectLocationButton.setText(SELECT_LOCATION_BUTTON_TITLE);
        selectLocationButton.setLayoutData(bgd);
        selectLocationButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.OPEN);
                dd.setFilterPath(getOSWorkspaceLocation());
                                
                String directoryPath = dd.open();
                if (directoryPath != null) {
                    projectLocation = directoryPath;
                    locationText.setText(directoryPath);
                }

                setFormErrorMessage();
            }
        });

        return locationText;
    }

    private void setFormErrorMessage() {
        boolean pageCompleteStatus = false;
        
        if (projectName.isEmpty()) {
            setErrorMessage(EMPTY_PROJECT_NAME_MESSAGE);
        } else if (projectLocation.isEmpty()) {
            setErrorMessage(EMPTY_PROJECT_LOCATION_MESSAGE);
        } else if (projectExists()) {
            setErrorMessage(PROJECT_EXISTS_MESSAGE);
        } else {
            setErrorMessage(null);
            pageCompleteStatus = true;
        }
        
        setPageComplete(pageCompleteStatus);
    }
    
    private boolean projectExists() {
        return false;
    }

    static String getOSWorkspaceLocation() {
        return ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
    }

}
