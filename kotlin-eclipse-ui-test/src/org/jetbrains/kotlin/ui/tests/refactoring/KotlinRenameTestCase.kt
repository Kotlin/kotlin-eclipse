package org.jetbrains.kotlin.ui.tests.refactoring

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils

public class KotlinRenameTestCase : KotlinProjectTestCase() {
    @Before
    fun before() {
        configureProjectWithStdLib()
    }
    
    protected fun doTest(testInfo: String) {
        val fileInfoText = KotlinTestUtils.getText(testInfo)
        val mainFile = InTextDirectivesUtils.findStringWithPrefixes(fileInfoText, "MAIN_FILE: ")
        val newName = InTextDirectivesUtils.findStringWithPrefixes(fileInfoText, "NEW_NAME: ")
        
    }
}