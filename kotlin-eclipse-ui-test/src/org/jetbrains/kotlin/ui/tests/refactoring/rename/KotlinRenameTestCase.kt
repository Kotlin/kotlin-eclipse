package org.jetbrains.kotlin.ui.tests.refactoring.rename

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils
import java.io.File
//import com.google.gson.JsonParser
//import com.google.gson.JsonObject
import org.eclipse.core.runtime.Path
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jface.text.TextSelection
import org.junit.Assert
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.ui.refactorings.rename.doRename
import org.eclipse.core.resources.IFile
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jdt.core.IType
import org.jetbrains.kotlin.ui.refactorings.rename.KotlinRenameAction
import org.jetbrains.kotlin.ui.refactorings.rename.doRename

abstract class KotlinRenameTestCase : KotlinProjectTestCase() {
    @Before
    fun before() {
        configureProjectWithStdLib()
    }
    
    protected fun doTest(testInfo: String) {
//        val fileInfoText = KotlinTestUtils.getText(testInfo)
//        val jsonParser = JsonParser()
//        
//        val renameObject = jsonParser.parse(fileInfoText) as JsonObject
//        
//        val rootFolder = Path(testInfo).removeLastSegments(1)
//        val loadedFiles = loadFiles(rootFolder.append("before").toFile())
//        
//        val editor = openMainFile(renameObject)
//        
//        val selection = findSelectionToRename(renameObject, editor)
//        Assert.assertTrue("Element to rename was not found", selection.getOffset() > 0)
//        
//        KotlinTestUtils.waitUntilIndexesReady()
//        
//        performRename(selection, renameObject["newName"].asString, editor)
//        
//        checkResult(rootFolder.append("after").toFile(), loadedFiles)
    }
    
    fun performRename(selection: ITextSelection, newName: String, editor: KotlinFileEditor) {
//        val jetElement = EditorUtil.getJetElement(editor, selection.getOffset())
//        if (jetElement == null) return
//        
//        val javaElements = resolveToJavaElements(jetElement, editor.javaProject!!)
//        if (javaElements.isEmpty()) return
//        
//        if (javaElements.size() > 1) {
//            throw RuntimeException("There are more than one element for ${jetElement.getText()}")
//        }
//        
//        doRename(javaElements[0] as IType, jetElement, newName, editor)
    }
    
    private fun checkResult(sourceFolderAfter: File, actualFiles: List<IFile>) {
        for (expectedFile in sourceFolderAfter.listFiles()) {
            val expectedSource = KotlinTestUtils.getText(expectedFile.getAbsolutePath())
            
            val actualFile = actualFiles.find { it.getName() == expectedFile.getName() }!!
            val actualSource = EditorUtil.getDocument(actualFile).get()
            
            Assert.assertEquals("${expectedFile.getName()} and ${actualFile.getName()} are not equals", expectedSource, actualSource)
        }
    }
    
//    private fun findSelectionToRename(renameObject: JsonObject, editor: KotlinFileEditor): TextSelection {
//        val document = editor.document
//        val position = document.get().indexOf(renameObject["oldName"].asString) + 1
//        return TextSelection(document, position, 0)
//    }
//    
//    private fun openMainFile(renameObject: JsonObject): KotlinFileEditor {
//        val mainFile = KotlinPsiManager.INSTANCE.getFilesByProject(getTestProject().getJavaProject().getProject()).find { 
//            it.getName() == renameObject["mainFile"].asString
//        }
//        val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
//        return IDE.openEditor(page, mainFile, false) as KotlinFileEditor
//    }
    
    private fun loadFiles(sourceRoot: File): List<IFile> {
        val loadedFiles = arrayListOf<IFile>()
        for (file in sourceRoot.listFiles()) {
            val fileContent = KotlinTestUtils.getText(file.getAbsolutePath())
            loadedFiles.add(createSourceFile(file.getName(), fileContent))
        }
        
        return loadedFiles
    }
}