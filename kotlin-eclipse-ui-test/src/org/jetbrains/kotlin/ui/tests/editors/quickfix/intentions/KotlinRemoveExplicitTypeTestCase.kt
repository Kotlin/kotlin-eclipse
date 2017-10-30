package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions

import org.jetbrains.kotlin.ui.editors.quickassist.KotlinRemoveExplicitTypeAssistProposal
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils

@Suppress("UPPER_BOUND_VIOLATED")
abstract class KotlinRemoveExplicitTypeTestCase : AbstractKotlinQuickAssistTestCase<KotlinRemoveExplicitTypeAssistProposal>() {
    fun doTest(testPath: String) {
		@Suppress("TYPE_MISMATCH", "MISSING_DEPENDENCY_CLASS")
        doTestFor(testPath) { KotlinRemoveExplicitTypeAssistProposal(it) }
    }
    
    override fun assertByEditor(editor: JavaEditor, expected: String) {
        EditorTestUtils.assertByEditor(editor, expected)
    }
}