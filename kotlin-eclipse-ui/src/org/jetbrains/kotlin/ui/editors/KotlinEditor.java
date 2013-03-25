package org.jetbrains.kotlin.ui.editors;

import org.eclipse.ui.editors.text.TextEditor;

public class KotlinEditor extends TextEditor {

	private KotlinColorManager colorManager;

	public KotlinEditor() {
		super();
		colorManager = new KotlinColorManager();
		setSourceViewerConfiguration(new KotlinConfiguration(colorManager));
		setDocumentProvider(new KotlinDocumentProvider());
	}
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}
