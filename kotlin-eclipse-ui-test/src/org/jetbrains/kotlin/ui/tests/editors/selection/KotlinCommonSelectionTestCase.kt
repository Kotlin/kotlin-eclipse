package org.jetbrains.kotlin.ui.tests.editors.selection

import java.io.File;
import java.util.ArrayList;

import org.jetbrains.kotlin.testframework.editor.KotlinEditorSequentialAutoTestCase;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorAutoTestCase;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;
import org.junit.Before

public abstract class KotlinCommonSelectionTestCase: KotlinEditorSequentialAutoTestCase() {
	private val INITIAL_FILE_NAME = "0" + KotlinEditorAutoTestCase.KT_FILE_EXTENSION;
	protected val COMMON_DIR: String = "wordSelection"
	abstract protected val RELATIVE_DIR: String
    
    @Before
    fun before() {
        configureProject()
    }

	override fun getInitialFileName() = INITIAL_FILE_NAME
	
	override fun getAfterFilesPaths(testFolder: File): ArrayList<String> {
		val afterFileContents = ArrayList<String>() 
		
		var fileIndex = 1
		
		while (true) {
			val filePath = testFolder.getAbsolutePath() + File.separator + fileIndex + KotlinEditorAutoTestCase.KT_FILE_EXTENSION
			val afterFile = File(filePath)
			if (!afterFile.exists()) {
				break;
			}
			afterFileContents.add(filePath)
			fileIndex++
		}
				
		return afterFileContents;
	}
	
	override fun getAfterFileContent(afterFilePath: String): String {
		return KotlinEditorTestCase.getText(afterFilePath).replace(KotlinEditorTestCase.CARET_TAG.toRegex(), "")
	}

	override fun getTestDataRelativePath() = COMMON_DIR + File.separator + RELATIVE_DIR
}