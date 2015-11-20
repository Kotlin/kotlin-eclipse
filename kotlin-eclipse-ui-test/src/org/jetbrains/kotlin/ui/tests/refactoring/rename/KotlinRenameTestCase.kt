package org.jetbrains.kotlin.ui.tests.refactoring.rename

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.testframework.utils.InTextDirectivesUtils
import java.io.File
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
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.eclipse.core.runtime.IPath
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider

abstract class KotlinRenameTestCase : KotlinProjectTestCase() {
    @Before
    fun before() {
        configureProjectWithStdLib()
    }
    
    protected fun doTest(testInfo: String) {
        val fileInfoText = KotlinTestUtils.getText(testInfo)
        val jsonParser = JsonParser()
        
        val renameObject = jsonParser.parse(fileInfoText) as JsonObject
        
        val rootFolder = Path(testInfo).removeLastSegments(1)
        val beforeSourceFolder = rootFolder.append("before")
        loadFiles(beforeSourceFolder.toFile())
        
        KotlinTestUtils.joinBuildThread()
        reconcileJavaFiles(beforeSourceFolder.toFile(), beforeSourceFolder)
        KotlinTestUtils.joinBuildThread()
        
        val editor = openMainFile(renameObject)
        
        val selection = findSelectionToRename(renameObject, editor)
        Assert.assertTrue("Element to rename was not found", selection.getOffset() > 0)
        
        performRename(selection, renameObject["newName"].asString, editor)
        
        val base = rootFolder.append("after")
        checkResult(base.toFile(), base)
    }
    
    fun performRename(selection: ITextSelection, newName: String, editor: KotlinFileEditor) {
        val selectedElement = EditorUtil.getJetElement(editor, selection.getOffset())
        if (selectedElement == null) return
        
        val sourceDeclaration = selectedElement.resolveToSourceDeclaration(editor.javaProject!!)
        doRename(sourceDeclaration, newName, editor)
    }
    
    private fun checkResult(sourceFolderAfter: File, base: IPath) {
        val actualFiles = KotlinPsiManager.INSTANCE.getFilesByProject(getTestProject().getJavaProject().getProject())
        for (expectedFile in sourceFolderAfter.listFiles()) {
            if (expectedFile.isFile()) {
                val expectedSource = KotlinTestUtils.getText(expectedFile.getAbsolutePath())
                val actualSource = if (expectedFile.extension == "kt") {
                    val actualFile = actualFiles.find { it.getName() == expectedFile.getName() }!!
                    EditorUtil.getDocument(actualFile).get()
                } else {
                    val relative = Path(expectedFile.getPath()).makeRelativeTo(base)
                    val element = getTestProject().getJavaProject().findElement(relative)
                    EditorUtil.getDocument(element.getResource() as IFile).get()
                }
                
                Assert.assertEquals("Expected and actual files for ${expectedFile.getName()} are not equals", expectedSource, actualSource)
            } else if (expectedFile.isDirectory()) {
                checkResult(expectedFile, base)
            }
        }
    }
    
    private fun findSelectionToRename(renameObject: JsonObject, editor: KotlinFileEditor): TextSelection {
        val document = editor.document
        val position = document.get().indexOf(renameObject["oldName"].asString) + 1
        return TextSelection(document, position, 0)
    }
    
    private fun openMainFile(renameObject: JsonObject): KotlinFileEditor {
        val mainFile = KotlinPsiManager.INSTANCE.getFilesByProject(getTestProject().getJavaProject().getProject()).find { 
            it.getName() == renameObject["mainFile"].asString
        }
        val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
        return IDE.openEditor(page, mainFile, false) as KotlinFileEditor
    }
    
    private fun reconcileJavaFiles(sourceFolder: File, base: IPath) {
        sourceFolder.listFiles().forEach { 
            when {
                it.isFile -> {
                    val relative = Path(it.getPath()).makeRelativeTo(base)
                    val element = getTestProject().getJavaProject().findElement(relative)
                    if (element is ICompilationUnit) {
                        element.becomeWorkingCopy(null)
                        element.reconcile(ICompilationUnit.NO_AST, true, null, null)
                        element.commitWorkingCopy(true, null)
                        element.discardWorkingCopy()
                    }
                }
                
                it.isDirectory -> reconcileJavaFiles(it, base)
            }
        }
    }
    
    private fun loadFiles(sourceRoot: File) {
        val loadedFiles = arrayListOf<IFile>()
        for (file in sourceRoot.listFiles()) {
            if (file.isFile()) {
                val fileContent = KotlinTestUtils.getText(file.getAbsolutePath())
                loadedFiles.add(createSourceFile(file.getName(), fileContent))
            } else if (file.isDirectory()) {
                loadFiles(file)
            }
        }
    }
}