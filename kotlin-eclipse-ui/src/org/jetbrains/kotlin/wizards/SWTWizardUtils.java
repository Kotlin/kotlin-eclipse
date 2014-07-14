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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SWTWizardUtils {

    private static final int NUM_COLUMNS = 4;
    private static final String LABEL_TEXT_SUFFIX = ": ";

    private static GridData createGridData(int horizontalSpan, boolean grabExcessHorizontalSpace) {
        GridData result = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        result.horizontalSpan = horizontalSpan;
        result.grabExcessHorizontalSpace = grabExcessHorizontalSpace;

        return result;
    }

    private static Button createButton(Composite parent, String text, SelectionListener selectionListener, int style,
            int horizontalSpan, boolean grabExcessHorizontalSpace) {
        Button result = new Button(parent, style);
        result.setText(text);
        result.setLayoutData(createGridData(horizontalSpan, grabExcessHorizontalSpace));
        if (selectionListener != null) {
            result.addSelectionListener(selectionListener);
        }

        return result;
    }

    private static GridLayout createGridLayout() {
        GridLayout result = new GridLayout();
        result.numColumns = NUM_COLUMNS;

        return result;
    }

    public static Composite createComposite(Composite parent) {
        Composite result = new Composite(parent, SWT.NONE);
        result.setFont(parent.getFont());
        result.setLayout(createGridLayout());

        return result;
    }

    public static Group createGroup(Composite parent, String text) {
        Group result = new Group(parent, SWT.SHADOW_ETCHED_IN);
        result.setText(text);
        result.setLayout(createGridLayout());
        result.setLayoutData(createGridData(NUM_COLUMNS, true));

        return result;
    }

    public static Button createCheckbox(Composite parent, String text) {
        return createButton(parent, text, null, SWT.CHECK, NUM_COLUMNS, true);
    }

    public static Label createLabel(Composite parent, String text) {
        Label result = new Label(parent, SWT.LEFT | SWT.WRAP);
        result.setText(text + LABEL_TEXT_SUFFIX);
        result.setLayoutData(createGridData(1, false));

        return result;
    }

    public static Text createText(Composite parent, String text) {
        Text result = new Text(parent, SWT.SINGLE | SWT.BORDER);
        result.setText(text);
        result.setLayoutData(createGridData(2, true));

        return result;
    }

    public static Button createButton(Composite parent, String text, SelectionListener selectionListener) {
        return createButton(parent, text, selectionListener, SWT.PUSH, 1, false);
    }

    public static Label createSeparator(Composite parent) {
        Label result = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
        result.setLayoutData(createGridData(NUM_COLUMNS, false));

        return result;
    }

    public static Label createEmptySpace(Composite parent) {
        return new Label(parent, SWT.NONE);
    }

}
