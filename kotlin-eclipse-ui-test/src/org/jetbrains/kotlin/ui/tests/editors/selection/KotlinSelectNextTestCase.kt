package org.jetbrains.kotlin.ui.tests.editors.selection

public abstract class KotlinSelectNextTestCase: KotlinCommonSelectionTestCase() {
	override val RELATIVE_DIR = "selectNext"
	override fun performSingleOperation() = testEditor.runSelectNextAction()
}