package org.jetbrains.kotlin.ui.editors

import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditor
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory
import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.StructureSelectHistoryAction
import org.eclipse.jdt.internal.ui.text.JavaColorManager
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.views.contentoutline.IContentOutlinePage
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil
import org.jetbrains.kotlin.ui.debug.KotlinToggleBreakpointAdapter
import org.jetbrains.kotlin.ui.editors.outline.KotlinOutlinePage
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectEnclosingAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectNextAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSelectPreviousAction
import org.jetbrains.kotlin.ui.editors.selection.KotlinSemanticSelectionAction
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor
import kotlin.lazy
import java.lang.Class
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.IJavaProject
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.psi.KtPsiFactory
import com.intellij.openapi.util.text.StringUtil
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.KtFile
import org.eclipse.ui.IEditorInput
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.KtElement
import org.eclipse.jface.text.source.SourceViewerConfiguration
import org.jetbrains.kotlin.core.references.FILE_PROJECT
import org.jetbrains.kotlin.ui.editors.navigation.KotlinOpenDeclarationAction

public class KotlinClassFileEditor : ClassFileEditor(), KotlinEditor {
    override fun isEditable() = false

    override val javaEditor = this

    override val parsedFile: KtFile 
        get() {
            val environment = KotlinEnvironment.getEnvironment(javaProject);
            val ideaProject = environment.getProject();
            val jetFile = KtPsiFactory(ideaProject).createFile(StringUtil.convertLineSeparators(document.get(),"\n"))
            jetFile.putUserData(FILE_PROJECT, javaProject)
            return jetFile
        }

    override val javaProject: IJavaProject
        get() = classFile.getJavaProject()

    private val colorManager = JavaColorManager()
    
    private val kotlinOutlinePage : KotlinOutlinePage by lazy {
        KotlinOutlinePage(this)
    }
    
    override public fun getAdapter(required: Class<*>) : Any? =
        when (required) {
            IContentOutlinePage::class.java -> kotlinOutlinePage
            IToggleBreakpointsTarget::class.java -> KotlinToggleBreakpointAdapter
            else -> super<ClassFileEditor>.getAdapter(required)
        }

    override public fun createPartControl(parent:Composite) {
        setSourceViewerConfiguration(Configuration(colorManager, this, getPreferenceStore()))
        super<ClassFileEditor>.createPartControl(parent)
    }

    override protected fun isMarkingOccurrences() = false

    override protected fun isTabsToSpacesConversionEnabled() =
            IndenterUtil.isSpacesForTabs()

    override protected fun createActions() {
        super<ClassFileEditor>.createActions()

        val selectionHistory = SelectionHistory(this)
        val historyAction = StructureSelectHistoryAction(this, selectionHistory)
        historyAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_LAST)
        setAction(KotlinSemanticSelectionAction.HISTORY, historyAction)
        selectionHistory.setHistoryAction(historyAction)

        setAction(KotlinOpenDeclarationAction.OPEN_EDITOR_TEXT, KotlinOpenDeclarationAction(this))

        setAction(KotlinSelectEnclosingAction.SELECT_ENCLOSING_TEXT, KotlinSelectEnclosingAction(this, selectionHistory))
        setAction(KotlinSelectPreviousAction.SELECT_PREVIOUS_TEXT, KotlinSelectPreviousAction(this, selectionHistory))
        setAction(KotlinSelectNextAction.SELECT_NEXT_TEXT, KotlinSelectNextAction(this, selectionHistory))
    }

    override public fun dispose() {
        colorManager.dispose()
        super<ClassFileEditor>.dispose()
    }

    override public fun setSelection(element:IJavaElement) {
        when (element) {
            is IClassFile -> revealClassInFile(findDeclarationInFile(element))
            else -> KotlinOpenEditor.revealKotlinElement(this, element)
        }
    }

    companion object {
        @JvmField public val EDITOR_ID:String = "org.jetbrains.kotlin.ui.editors.KotlinClassFileEditor"
    }

    private val classFile: IClassFile
        get() = getEditorInput().getAdapter(IJavaElement::class.java) as IClassFile

    override val document: IDocument
        get() = getDocumentProvider().getDocument(getEditorInput())
    
    private fun findDeclarationInFile(classFile: IClassFile): KtClassOrObject? {
        val fqName = classFile.getType().getFullyQualifiedName()
        return parsedFile.accept(object: KtVisitor<KtClassOrObject?, String>() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject, classFqName: String): KtClassOrObject? =
                    if (classOrObject.getFqName().toString() == classFqName) {
                        classOrObject
                    } else {
                        super.visitClassOrObject(classOrObject, classFqName)
                    }
            
            override fun visitKtElement(element: KtElement, classFqName: String): KtClassOrObject? =
                    element.getChildren().asSequence().map { 
                        when(it) {
                            is KtElement -> it.accept(this, classFqName)
                            else -> null
                        }
                     }.filterNotNull().firstOrNull()
                
            override fun visitKtFile(file: KtFile, classFqName: String): KtClassOrObject? = 
                    visitKtElement(file, classFqName)
        }, fqName)
    }

    private fun revealClassInFile(classDeclaration: KtClassOrObject?) {
        if (classDeclaration != null) {
            val correctedOffset = LineEndUtil.convertLfToDocumentOffset(parsedFile.getText(), classDeclaration.getTextOffset(), document);
            selectAndReveal(correctedOffset, 0);
        }
    }
    
    //overriden for the purpose of highlighting
    //because JavaEditor::doSetInput resets source viewer configuration
    override fun setSourceViewerConfiguration(configuration: SourceViewerConfiguration) {
        when(configuration) {
            is Configuration -> super<ClassFileEditor>.setSourceViewerConfiguration(configuration)
            else -> super<ClassFileEditor>.setSourceViewerConfiguration(Configuration(colorManager, this, getPreferenceStore()))
        }
    }
    
    //overriden to empty because ClassFileEditor's semantic highlighting crashes Kotlin syntax coloring
    //TODO: rewrite when the semantic highlighting will be introduced
    override protected fun installSemanticHighlighting() {
        
    }
}