package org.jetbrains.kotlin.ui.tests.editors.quickfix.intentions

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.jetbrains.kotlin.testframework.utils.FileReaderHolder
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.quickassist.KotlinQuickAssistProposal
import org.junit.Assert
import org.junit.Before
import java.io.File

abstract class AbstractKotlinQuickAssistTestCase<Proposal : KotlinQuickAssistProposal>
    : KotlinProjectTestCase(), FileReaderHolder by FileReaderHolder() {
    @Before
    open fun configure() {
        configureProject()
    }

    @JvmOverloads
    protected fun doTestFor(
        testPath: String,
        joinBuildThread: Boolean = false,
        createProposal: (KotlinEditor) -> KotlinQuickAssistProposal
    ) {
        val fileText = fileReader(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        val proposal = createProposal.invoke(testEditor.editor as KotlinEditor)

        val isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "IS_APPLICABLE: ")
        var isApplicableExpected = isApplicableString == null || isApplicableString == "true"

        val pathToExpectedFile = "$testPath.after"
        val expectedFile = File(pathToExpectedFile)
        if (!expectedFile.exists()) isApplicableExpected = false

        if (joinBuildThread) {
            KotlinTestUtils.joinBuildThread()
        }

        Assert.assertTrue(
            "isAvailable() for " + proposal.javaClass + " should return " + isApplicableExpected,
            isApplicableExpected == proposal.isApplicable()
        )

        val shouldFailString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "SHOULD_FAIL_WITH: ")

        if (isApplicableExpected) {
            proposal.apply(testEditor.editor.viewer.document)

            if (shouldFailString == null) {
                assertByEditor(testEditor.editor, KotlinTestUtils.getText(pathToExpectedFile))
            }
        }
    }

    protected abstract fun assertByEditor(editor: JavaEditor, expected: String)
}
