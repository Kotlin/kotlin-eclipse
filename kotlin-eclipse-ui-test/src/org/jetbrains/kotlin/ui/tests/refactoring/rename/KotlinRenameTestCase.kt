package org.jetbrains.kotlin.ui.tests.refactoring.rename

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jface.text.ITextSelection
import org.eclipse.jface.text.TextSelection
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor
import org.jetbrains.kotlin.ui.refactorings.rename.createRenameSupport
import org.jetbrains.kotlin.ui.refactorings.rename.doRename
import org.junit.Assert
import org.junit.Before
import java.io.File

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
        val loadedFiles = arrayListOf<IFile>()
        loadFiles(beforeSourceFolder.toFile()) { loadedFiles.add(it) }
        
        KotlinTestUtils.joinBuildThread()
        reconcileJavaFiles(beforeSourceFolder.toFile(), beforeSourceFolder)
        KotlinTestUtils.joinBuildThread()
        
        val editor = openMainFile(renameObject, loadedFiles)
        
        val selection = findSelectionToRename(renameObject, editor)
        Assert.assertTrue("Element to rename was not found", selection.getOffset() > 0)
        
        val newName = renameObject["newName"].asString
        when (editor) {
            is KotlinCommonEditor -> performRename(selection, newName, editor)
            else -> performRenameFromJava(selection, newName, editor as JavaEditor)
        }
        
        val base = rootFolder.append("after")
        checkResult(base.toFile(), base)
    }
    
    fun performRename(selection: ITextSelection, newName: String, editor: KotlinCommonEditor) {
        val selectedElement = EditorUtil.getJetElement(editor, selection.getOffset())
        if (selectedElement == null) return
        
        val sourceDeclaration = selectedElement.resolveToSourceDeclaration()
        doRename(sourceDeclaration, newName, editor)
    }
    
    fun performRenameFromJava(selection: ITextSelection, newName: String, editor: JavaEditor) {
        val root = EditorUtility.getEditorInputJavaElement(editor, false) as ICompilationUnit
        val javaElement = root.codeSelect(selection.getOffset(), 0)[0]
        
        val renameSupport = createRenameSupport(javaElement, newName)
        renameSupport.perform(editor.getSite().getShell(), editor.getSite().getWorkbenchWindow())
    }
    
    private fun checkResult(sourceFolderAfter: File, base: IPath) {
        val actualFiles = KotlinPsiManager.getFilesByProject(getTestProject().getJavaProject().getProject())
        for (expectedFile in sourceFolderAfter.listFiles()) {
            if (expectedFile.isFile()) {
                val expectedSource = KotlinTestUtils.getText(expectedFile.getAbsolutePath())
                val actualSource = if (expectedFile.extension == "kt") {
                    val actualFile = actualFiles.find { it.getName() == expectedFile.getName() }!!
                    EditorUtil.getDocument(actualFile).get()
                } else if (expectedFile.extension == "kts") {
                    val actualFile = getTestProject().getJavaProject().getProject().findMember("src/${expectedFile.getName()}")
                    EditorUtil.getDocument(actualFile as IFile).get()
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
    
    private fun findSelectionToRename(renameObject: JsonObject, editor: IEditorPart): TextSelection {
        val document = (editor as JavaEditor).getDocumentProvider().getDocument(editor.getEditorInput())
        val position = document.get().indexOf(renameObject["oldName"].asString) + 1
        return TextSelection(document, position, 0)
    }
    
    private fun openMainFile(renameObject: JsonObject, loadedFiles: List<IFile>): IEditorPart {
        val mainFile = loadedFiles.find { 
            it.getName() == renameObject["mainFile"].asString
        }
        val page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
        return IDE.openEditor(page, mainFile, false)
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
    
    private fun loadFiles(sourceRoot: File, action: (IFile) -> Unit) {
        for (file in sourceRoot.listFiles()) {
            if (file.isFile()) {
                val fileContent = KotlinTestUtils.getText(file.getAbsolutePath())
                action(createSourceFile(file.getName(), fileContent))
            } else if (file.isDirectory()) {
                loadFiles(file, action)
            }
        }
    }
}