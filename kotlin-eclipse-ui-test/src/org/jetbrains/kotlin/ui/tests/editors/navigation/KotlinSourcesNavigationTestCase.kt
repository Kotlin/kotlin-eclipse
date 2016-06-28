package org.jetbrains.kotlin.ui.tests.editors.navigation

import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction
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
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFunction

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
        
        val initialFile = KotlinPsiManager.getParsedFile(editorFile)
        val editor = testEditor.getEditor()
        testEditor.setCaret(referenceOffset)
        
        val openAction = KotlinOpenDeclarationAction(editor as KotlinFileEditor)
        openAction.run(TextSelection(KotlinTestUtils.getCaret(editor), 0))
        
        val activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor() as KotlinEditor
        assertWithEditor(initialFile, activeEditor)
    }

    private fun assertWithEditor(initialFile: KtFile, editor: KotlinEditor) {
        val fileText = initialFile.getText()
        val expectedFile = InTextDirectivesUtils.findStringWithPrefixes(fileText, "SRC:");
        val expectedTarget = InTextDirectivesUtils.findStringWithPrefixes(fileText, "TARGET:");
        
        Assert.assertEquals(expectedFile, editor.javaEditor.getTitleToolTip())
        
        val editorOffset = editor.javaEditor.getViewer().getTextWidget().getCaretOffset()
        val offsetInPSI = LineEndUtil.convertCrToDocumentOffset(editor.document, editorOffset)
        val psiElement = getParsedFile(editor).findElementAt(offsetInPSI)
        val declaration = psiElement?.getNonStrictParentOfType(PsiNamedElement::class.java) as KtNamedDeclaration
        
        val expressionName = getPresentableString(declaration)
        val locationString = if (declaration is KtConstructor<*>) {
            val name = declaration.getContainingClassOrObject().fqName
            "(in $name)"
        } else {
            getLocationString(declaration)
        }
        
        Assert.assertEquals(expectedTarget, "$locationString.$expressionName")
    }
    
    abstract fun getParsedFile(editor: KotlinEditor): KtFile
    
    private fun getReferenceOffset(fileText: String) = fileText.indexOf(KotlinEditorTestCase.CARET_TAG)
    
    private fun getPresentableString(declaration: KtNamedDeclaration): String {
        if (declaration !is KtFunction) {
            return declaration.name!!
        }
        
        return buildString {
            declaration.name?.let { append(it) }
            
            append("(")
            append(declaration.valueParameters.joinToString { it.typeReference?.text ?: "" })
            append(")")
        }
    }
    
    private fun getLocationString(declaration: KtNamedDeclaration): String? {
        val name = declaration.fqName ?: return null
        val receiverTypeRef = (declaration as? KtCallableDeclaration)?.receiverTypeReference
        if (receiverTypeRef != null) {
            return "(for " + receiverTypeRef.text + " in " + name.parent() + ")"
        } else if (declaration.parent is KtFile) {
            return "(" + name.parent() + ")"
        } else {
            return "(in " + name.parent() + ")"
        }
    }
}