package org.jetbrains.kotlin.ui.tests.editors.navigation

import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.KotlinOpenDeclarationAction
import org.eclipse.jface.text.TextSelection
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.psi.KtFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiComment
import java.io.File
import org.junit.Assert
import org.jetbrains.kotlin.ui.editors.KotlinClassFileEditor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.junit.Before
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.rules.TestName
import org.junit.Rule
import org.jetbrains.kotlin.testframework.editor.TextEditorTest

abstract class KotlinSourcesNavigationTestCase: KotlinProjectTestCase() {
    
    abstract val testDataPath: String
    
    companion object {
        private val KT_FILE_EXTENSION = ".kt"
        private val TEST_PREFIX = "test"
    }
    
    private val name: TestName = TestName()
    
    //TODO: remove this workaround when access to public fields will be implemented
    @Rule
    public fun getTestName(): TestName = name
    
    private val testEditor: TextEditorTest by lazy {
        val processedFileText = KotlinEditorTestCase.removeTags(fileText)
        configureEditor(inputFileName, processedFileText)
    }
    
    val inputFileName: String by lazy {
        val inputFileName = name.getMethodName()
        val filenameAsArray = inputFileName.substring(TEST_PREFIX.length).toCharArray()
        filenameAsArray[0] = Character.toLowerCase(filenameAsArray[0])
        String(filenameAsArray) + KT_FILE_EXTENSION
    }
    
    val fileText: String by lazy {
        KotlinEditorTestCase.getText(testDataPath  + File.separator + inputFileName)
    }
    
    val processedFileText: String by lazy {
        KotlinEditorTestCase.removeTags(fileText)
    }
    
    @Before    
    open fun configure() {
        configureProject()
        createSourceFile(inputFileName, processedFileText)
    }

    public fun doAutoTest() {
        val testEditor = configureEditor(inputFileName, processedFileText)
        val referenceOffset = getReferenceOffset(fileText)
        val editorFile = testEditor.getEditingFile()
        
        val initialFile = KotlinPsiManager.INSTANCE.getParsedFile(editorFile)
        val editor = testEditor.getEditor()
        testEditor.setCaret(referenceOffset)
        
        val openAction = KotlinOpenDeclarationAction(editor as KotlinFileEditor)
        openAction.run(TextSelection(KotlinTestUtils.getCaret(editor), 0))
        
        val activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() as KotlinEditor
        assertWithEditor(initialFile, activeEditor)
    }

    private fun assertWithEditor(initialFile: KtFile, editor: KotlinEditor) {
        val comments = PsiTreeUtil.getChildrenOfTypeAsList(initialFile, PsiComment::class.java)
        val expectedTarget = comments.get(comments.size - 1).getText().substring(2).split(":")
        Assert.assertEquals(2, expectedTarget.size)
        val expectedFile = expectedTarget[0]
        val expectedName = expectedTarget[1]
        
        val parsedFile = getParsedFile(editor)
        
        val editorOffset = editor.javaEditor.getViewer().getTextWidget().getCaretOffset()
        
        val expression = parsedFile.findElementAt(editorOffset)?.getNonStrictParentOfType(PsiNamedElement::class.java)
        
        Assert.assertEquals(expectedFile, editor.javaEditor.getTitleToolTip())
        Assert.assertEquals(expectedName, expression?.getName())
    }
    
    abstract fun getParsedFile(editor: KotlinEditor): KtFile
    
    private fun getReferenceOffset(fileText: String) =
            fileText.indexOf(KotlinEditorTestCase.REFERENCE_TAG);
}