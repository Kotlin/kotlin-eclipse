package org.jetbrains.kotlin.ui.tests.refactoring.rename

import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase
import org.junit.Before
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.refactorings.rename.doRename
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.core.references.VisibilityScopeDeclaration

abstract class KotlinLocalRenameTestCase : KotlinProjectTestCase() {
    @Before
    fun before() {
        configureProjectWithStdLib()
    }
    
    protected fun doTest(testPath: String, newName: String) {
        val fileText = KotlinTestUtils.getText(testPath)
        val testEditor = configureEditor(KotlinTestUtils.getNameByPath(testPath), fileText)
        
        val jetElement = EditorUtil.getJetElement(testEditor.getEditor() as KotlinFileEditor, 
                KotlinTestUtils.getCaret(testEditor.getEditor()))!!
        
        doRename(VisibilityScopeDeclaration.KotlinOnlyScopeDeclaration(jetElement as JetDeclaration), newName, 
                testEditor.getEditor() as KotlinFileEditor)
        
        val expected = KotlinTestUtils.getText("${testPath}.after")
        
        EditorTestUtils.assertByEditor(testEditor.getEditor(), expected)
    }
}