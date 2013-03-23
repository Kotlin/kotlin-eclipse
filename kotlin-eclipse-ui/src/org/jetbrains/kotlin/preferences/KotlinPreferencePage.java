package org.jetbrains.kotlin.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class KotlinPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    public KotlinPreferencePage() {
    }

    public KotlinPreferencePage(String title) {
        super(title);
    }

    public KotlinPreferencePage(String title, ImageDescriptor image) {
        super(title, image);
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected Control createContents(Composite parent) {
        return null;
    }

}
