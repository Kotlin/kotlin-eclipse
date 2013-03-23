package org.jetbrains.kotlin.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class KotlinCompilerPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    public KotlinCompilerPreferencePage() {
        // TODO Auto-generated constructor stub
    }

    public KotlinCompilerPreferencePage(String title) {
        super(title);
        // TODO Auto-generated constructor stub
    }

    public KotlinCompilerPreferencePage(String title, ImageDescriptor image) {
        super(title, image);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void init(IWorkbench workbench) {
        // TODO Auto-generated method stub

    }

    @Override
    protected Control createContents(Composite parent) {
        // TODO Auto-generated method stub
        return null;
    }

}
