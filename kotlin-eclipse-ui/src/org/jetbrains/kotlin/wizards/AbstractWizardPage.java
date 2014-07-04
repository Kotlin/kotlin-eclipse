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

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public abstract class AbstractWizardPage extends WizardPage implements IWizardPage {

    protected AbstractWizardPage(String title, String description) {
        super(title);
        super.setTitle(title);
        super.setDescription(description);
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

        setPageComplete(false);
    }

    protected abstract void createControls(Composite composite);

    protected abstract String createErrorMessage();

    protected final void validate() {
        String errorMessage = createErrorMessage();

        setErrorMessage(errorMessage);
        setPageComplete(errorMessage == null);
    }

    protected static IWorkspaceRoot getWorkspaceRoot() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }

    protected static GridData createGridData(int horizontalSpan, boolean grabExcessHorizontalSpace) {
        GridData result = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        result.horizontalSpan = horizontalSpan;
        result.grabExcessHorizontalSpace = grabExcessHorizontalSpace;

        return result;
    }

    protected static Label createLabel(Composite parent, String text, Object layoutData) {
        Label result = new Label(parent, SWT.LEFT | SWT.WRAP);
        result.setText(text);
        result.setLayoutData(layoutData);

        return result;
    }

    protected static Text createText(Composite parent, String text, Object layoutData) {
        Text result = new Text(parent, SWT.SINGLE | SWT.BORDER);
        result.setText(text);
        result.setLayoutData(layoutData);

        return result;
    }

    protected static Button createButton(Composite parent, String text, Object layoutData,
            SelectionListener selectionListener) {
        Button result = new Button(parent, SWT.PUSH);
        result.setText(text);
        result.setLayoutData(layoutData);
        result.addSelectionListener(selectionListener);

        return result;
    }

    protected static Label createEmptySpace(Composite composite) {
        return new Label(composite, SWT.NONE);
    }

}
