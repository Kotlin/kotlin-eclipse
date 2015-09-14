package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions

import org.jetbrains.kotlin.ui.editors.quickassist.KotlinRemoveExplicitTypeAssistProposal
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils

abstract class KotlinRemoveExplicitTypeTestCase : AbstractKotlinQuickAssistTestCase<KotlinRemoveExplicitTypeAssistProposal>() {
    fun doTest(testPath: String) {
        doTestFor(testPath, KotlinRemoveExplicitTypeAssistProposal())
    }
    
    override fun assertByEditor(editor: JavaEditor, expected: String) {
        EditorTestUtils.assertByEditor(editor, expected)
    }
}