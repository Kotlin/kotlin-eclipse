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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SWTWizardUtils {

    private static final String LABEL_TEXT_SUFFIX = ": ";

    private static GridData createGridData(int horizontalSpan, boolean grabExcessHorizontalSpace) {
        GridData result = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        result.horizontalSpan = horizontalSpan;
        result.grabExcessHorizontalSpace = grabExcessHorizontalSpace;

        return result;
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
        Button result = new Button(parent, SWT.PUSH);
        result.setText(text);
        result.setLayoutData(createGridData(1, false));
        result.addSelectionListener(selectionListener);

        return result;
    }

    public static Label createSeparator(Composite parent) {
        Label result = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
        result.setLayoutData(createGridData(4, false));

        return result;
    }

    public static Label createEmptySpace(Composite parent) {
        return new Label(parent, SWT.NONE);
    }

}
