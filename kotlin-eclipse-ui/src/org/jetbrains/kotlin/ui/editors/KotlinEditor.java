package org.jetbrains.kotlin.ui.editors;

import org.eclipse.ui.editors.text.TextEditor;

public class KotlinEditor extends TextEditor {

    private ColorManager colorManager;

    public KotlinEditor() {
        super();
        colorManager = new ColorManager();
        setSourceViewerConfiguration(new Configuration(colorManager));
        setDocumentProvider(new DocumentProvider());
    }

    @Override
    public void dispose() {
        colorManager.dispose();
        super.dispose();
    }

}
