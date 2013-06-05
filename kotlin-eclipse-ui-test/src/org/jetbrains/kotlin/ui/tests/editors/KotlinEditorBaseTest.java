package org.jetbrains.kotlin.ui.tests.editors;

import junit.framework.Assert;

import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.junit.Test;

public class KotlinEditorBaseTest {
	@Test
	public void sampleTest() {
		KotlinEditor kotlinEditor = new KotlinEditor();
		Assert.assertNotNull(kotlinEditor);
	}
}