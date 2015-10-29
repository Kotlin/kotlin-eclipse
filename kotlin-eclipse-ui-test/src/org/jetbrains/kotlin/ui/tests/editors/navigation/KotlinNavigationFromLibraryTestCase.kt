package org.jetbrains.kotlin.ui.tests.editors.navigation

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.rules.TestName
import org.junit.Rule
import org.junit.Before
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.JavaModelException
import org.junit.AfterClass
import org.jetbrains.kotlin.ui.editors.KotlinClassFileEditor
import org.jetbrains.kotlin.ui.tests.editors.navigation.library.getTestLibrary;
import org.jetbrains.kotlin.ui.tests.editors.navigation.library.clean;
import org.eclipse.jdt.core.IPackageFragment
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor
import org.jetbrains.kotlin.psi.KtNamedFunction
import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import org.junit.Assert
import org.eclipse.jdt.core.IMember
import org.jetbrains.kotlin.ui.editors.KotlinOpenDeclarationAction
import com.intellij.psi.PsiNamedElement
import org.eclipse.ui.PlatformUI
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.ui.IEditorPart
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.ui.tests.editors.navigation.library.TestLibraryData

open public class KotlinNavigationFromLibraryTestCase: KotlinProjectTestCase() {

    private val name: TestName = TestName()

    //TODO: remove this workaround when access to public fields will be implemented
    @Rule
    public fun getTestName(): TestName = name

    @Before
    fun configure() {
        configureProject()
        val testData = getTestLibrary()
        val libPath = Path(testData.libPath)
        val srcPath = Path(testData.srcPath)
        try {
            getTestProject().addLibrary(libPath, srcPath)
        } catch (e: JavaModelException) {
            throw RuntimeException(e)
        }
    }

    public fun getEditor(testClassName: String): IEditorPart {
        val cls = (getTestProject().getJavaProject().findType(testClassName) as IMember).getClassFile()
        return JavaUI.openInEditor(cls, true, true) //activate = true, reveal = true
    }

    public fun configureEditor(editor: KotlinClassFileEditor, testData: TestData) {
        val functionBody = testData.functionToTest.getBodyExpression()!!

        val offset = functionBody.getTextOffset() + functionBody.getText().indexOf(testData.referenceText)
        val start = LineEndUtil.convertLfToDocumentOffset(editor.parsedFile.getText(), offset, editor.document)

        editor.selectAndReveal(start, 0)
    }

    public fun doAutoTest(testClassName: String) {
        val editor = getEditor(testClassName)

        Assert.assertNotNull("Editor must be not null", editor)
        Assert.assertTrue("Editor must be of KotlinClassFileEditor type", editor is KotlinClassFileEditor)

        val kotlinEditor = editor as KotlinClassFileEditor

        val functionName = name.getMethodName()
        val testData = getTestData(editor.parsedFile, functionName)

        configureEditor(kotlinEditor, testData)

        runOpenDeclarationAction(kotlinEditor)

        val activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() as KotlinEditor

        assertWithEditor(activeEditor, testData.targetFilePath, testData.targetSymbol)
    }

    private fun getTestData(parsedFile: KtFile, functionName: String): TestData {
        val functionToTest = parsedFile.findChildrenByClass(KtNamedFunction::class.java)
            .filter {functionName == it.name}.firstOrNull()
        Assert.assertNotNull("Can't find a function to test: "+functionName, functionName)

        val comments = PsiTreeUtil.getChildrenOfType(functionToTest, PsiComment::class.java)
            ?.map {it.getText().substring(2).trim()}
        Assert.assertNotNull("Test function must provide comments with test info: "+ functionName, comments)

        val refText = comments!!.getOrNull(0)
        Assert.assertNotNull("Expected a reference text in the function comments: "+functionName, refText)
        
        val expectedTarget = comments.get(1).split(":")

        val expectedFile = expectedTarget.getOrNull(0)
        Assert.assertNotNull("Expected a file name in the function comments: "+ functionName, expectedFile)

        val expectedName = expectedTarget.getOrNull(1)
        Assert.assertNotNull("Expected referenced name in the function comments: "+ functionName, expectedName)

        return TestData(functionToTest!!, refText!!, expectedFile!!, expectedName!!)
    }

    private fun assertWithEditor(editor: KotlinEditor, expectedFile: String, expectedName: String) {
        val parsedFile = editor.parsedFile!!

        val editorOffset = editor.javaEditor.getViewer().getTextWidget().getCaretOffset()

        val expression = PsiTreeUtil.getNonStrictParentOfType(parsedFile.findElementAt(editorOffset)!!, PsiNamedElement::class.java)

        Assert.assertEquals(expectedFile, editor.javaEditor.getTitleToolTip())
        Assert.assertEquals(expectedName, expression?.getName())
    }

    private fun runOpenDeclarationAction(editor: KotlinClassFileEditor) {
        editor.getAction(KotlinOpenDeclarationAction.OPEN_EDITOR_TEXT).run();
    }

    companion object {
        @AfterClass
        @JvmStatic
        fun afterAllTests() {
            getTestLibrary().clean()
        }
    }

    private data class TestData(val functionToTest: KtNamedFunction,
                                val referenceText: String,
                                val targetFilePath: String,
                                val targetSymbol: String)
}