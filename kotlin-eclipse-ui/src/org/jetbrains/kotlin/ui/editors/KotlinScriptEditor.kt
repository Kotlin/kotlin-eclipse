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
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import com.intellij.openapi.util.text.StringUtilRt
import org.jetbrains.kotlin.core.model.KotlinScriptEnvironment
import org.eclipse.core.resources.IResourceDelta

class KotlinScriptEditor : KotlinCommonEditor() {
    override val parsedFile: KtFile?
        get() {
            val file = eclipseFile ?: return null
            return KotlinPsiManager.getKotlinFileIfExist(file, document.get())
        }

    override val javaProject: IJavaProject? by lazy {
        eclipseFile?.let { JavaCore.create(it.getProject()) }
    }

    override val document: IDocument
        get() = getDocumentProvider().getDocument(getEditorInput())
    
    override val isScript: Boolean
        get() = true
    
    override fun dispose() {
        super.dispose()
        
        eclipseFile?.let {
            KotlinScriptEnvironment.removeKotlinEnvironment(it)
            KotlinPsiManager.updateProjectPsiSources(it, IResourceDelta.REMOVED)
        }
    }
}