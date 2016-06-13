package org.jetbrains.kotlin.ui.editors

import com.intellij.openapi.util.text.StringUtil
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.eclipse.jdt.core.IJavaProject

class KotlinScriptEditor : CompilationUnitEditor(), KotlinEditor {
    
    private val kotlinReconcilingStrategy = KotlinReconcilingStrategy(this)
    
    override val javaEditor: JavaEditor
        get() = this

    override val parsedFile: KtFile?
        get() {
            if (javaProject == null) return null
            
            val environment = KotlinEnvironment.getEnvironment(javaProject!!)
            val ideaProject = environment.getProject()
            val jetFile = KtPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(document.get(), "\n"))
            
            return jetFile
        }

    override val javaProject: IJavaProject? by lazy {
        eclipseFile?.let { JavaCore.create(it.getProject()) }
    }

    override val document: IDocument
        get() = getDocumentProvider().getDocument(getEditorInput())
    
    override val eclipseFile: IFile?
        get() = getEditorInput().getAdapter(IFile::class.java)
}