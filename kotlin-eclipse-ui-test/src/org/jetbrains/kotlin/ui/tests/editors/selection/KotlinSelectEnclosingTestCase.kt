package org.jetbrains.kotlin.ui.tests.editors.selection

public abstract class KotlinSelectEnclosingTestCase: KotlinCommonSelectionTestCase() {
	override val RELATIVE_DIR = "selectEnclosing"
	override fun performSingleOperation() = testEditor.runSelectEnclosingAction()
}