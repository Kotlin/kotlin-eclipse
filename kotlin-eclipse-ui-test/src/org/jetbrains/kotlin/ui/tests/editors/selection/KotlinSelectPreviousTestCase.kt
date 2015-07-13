package org.jetbrains.kotlin.ui.tests.editors.selection

public abstract class KotlinSelectPreviousTestCase: KotlinCommonSelectionTestCase() {
	override val RELATIVE_DIR = "selectPrevious"
	override fun performSingleOperation() = testEditor.runSelectPreviousAction()
}