package org.jetbrains.kotlin.ui.editors

import com.intellij.openapi.util.text.StringUtil
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.eclipse.jdt.internal.ui.text.JavaColorManager
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextViewerExtension
import org.eclipse.swt.widgets.Composite
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.ui.editors.annotations.KotlinLineAnnotationsReconciler
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage

class KotlinScriptEditor : CompilationUnitEditor(), KotlinEditor {
    
    private val kotlinReconcilingStrategy = KotlinReconcilingStrategy(this)
    
    private val colorManager: IColorManager = JavaColorManager()
    
    private val kotlinOutlinePage = KotlinOutlinePage(this)
    
    private val bracketInserter: KotlinBracketInserter = KotlinBracketInserter()
    
    override public fun createPartControl(parent: Composite) {
        setSourceViewerConfiguration(FileEditorConfiguration(colorManager, this, getPreferenceStore(), kotlinReconcilingStrategy))
        kotlinReconcilingStrategy.addListener(KotlinLineAnnotationsReconciler)
        kotlinReconcilingStrategy.addListener(kotlinOutlinePage)
        
        super<CompilationUnitEditor>.createPartControl(parent)
        
        val sourceViewer = getSourceViewer()
        if (sourceViewer is ITextViewerExtension) {
            bracketInserter.setSourceViewer(sourceViewer)
            bracketInserter.addBrackets('{', '}')
            sourceViewer.prependVerifyKeyListener(bracketInserter)
        }
    }
    
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