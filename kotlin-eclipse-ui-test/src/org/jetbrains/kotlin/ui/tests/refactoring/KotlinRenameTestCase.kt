package org.jetbrains.kotlin.ui.tests.refactoring

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import java.io.File

public class KotlinRenameTestCase : KotlinProjectTestCase() {
    @Before
    fun before() {
        configureProjectWithStdLib()
    }
    
    protected fun doTest(testInfo: String) {
        val fileInfoText = KotlinTestUtils.getText(testInfo)
        val jsonParser = JsonParser()
        
        val renameObject = jsonParser.parse(fileInfoText) as JsonObject
    }
    
    private fun loadFiles(sourceRoot: File, renameObject: JsonObject) {
        
    }
}