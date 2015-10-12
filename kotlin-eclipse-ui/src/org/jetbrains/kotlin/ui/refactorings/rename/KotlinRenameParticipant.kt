package org.jetbrains.kotlin.ui.refactorings.rename

import org.eclipse.ltk.core.refactoring.participants.RenameParticipant
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.Change
import kotlin.properties.Delegates
import org.eclipse.jdt.core.IType
import java.util.ArrayList
import org.eclipse.ltk.core.refactoring.CompositeChange
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant
import org.eclipse.search.ui.text.Match
import org.eclipse.jdt.ui.search.ElementQuerySpecification
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.core.runtime.NullProgressMonitor
import org.jetbrains.kotlin.ui.search.KotlinElementMatch
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.ltk.core.refactoring.TextFileChange
import org.jetbrains.kotlin.eclipse.ui.utils.getTextDocumentOffset
import org.eclipse.text.edits.ReplaceEdit
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments
import org.eclipse.ltk.core.refactoring.participants.RenameArguments
import org.eclipse.text.edits.TextEdit
import org.eclipse.core.resources.IFile
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.eclipse.jdt.core.IJavaElement

open class KotlinRenameParticipant : RenameParticipant() {
    lateinit var element: IJavaElement
    lateinit var newName: String
    
    val changes = arrayListOf<Change>()
        
    override fun initialize(element: Any): Boolean {
        this.element = obtainOriginElement(element as IJavaElement)
        changes.clear()
        return true
    }
    
    override fun initialize(processor: RefactoringProcessor, element: Any?, arguments: RefactoringArguments): Boolean {
        if (arguments is RenameArguments) {
            newName = arguments.getNewName()
        }
        
        return super.initialize(processor, element, arguments)
    }
    
    override fun checkConditions(pm: IProgressMonitor, context: CheckConditionsContext): RefactoringStatus? {
        val kotlinQueryParticipant = KotlinQueryParticipant()
        val matches = arrayListOf<Match>()
        val factory = JavaSearchScopeFactory.getInstance()
        val querySpecification = ElementQuerySpecification(
                element, 
                IJavaSearchConstants.ALL_OCCURRENCES,
                factory.createWorkspaceScope(false),
                factory.getWorkspaceScopeDescription(false))
        
        kotlinQueryParticipant.search({ matches.add(it) }, querySpecification, NullProgressMonitor())
        
        val groupedEdits = matches
            .map { createTextChange(it) }
            .filterNotNull()
            .groupBy { it.file }
        
        for ((file, edits) in groupedEdits) {
            val fileChange = TextFileChange("Kotlin change", file)
            edits.forEach { TextChangeCompatibility.addTextEdit(fileChange, "Kotlin change", it.edit) }
            
            changes.add(fileChange)
        }
        
        return RefactoringStatus() // TODO: add corresponding refactoring status
    }
    
    override fun getName() = "Kotlin Type Rename Participant"
    
    override fun createChange(pm: IProgressMonitor): Change {
        return CompositeChange("Changes in Kotlin", changes.toTypedArray())
    }
    
    private fun createTextChange(match: Match): FileEdit? {
        if (match !is KotlinElementMatch) return null
        
        val jetElement = match.jetElement
        
        val eclipseFile = KotlinPsiManager.getEclispeFile(jetElement.getContainingJetFile())
        if (eclipseFile == null) return null
        
        val document = EditorUtil.getDocument(eclipseFile) // TODO: make workaround here later
        
        val textLength = if (jetElement is JetNamedDeclaration) {
            jetElement.getNameIdentifier()!!.getTextLength()
        } else {
            jetElement.getTextLength()
        }
        
        return FileEdit(
                eclipseFile, 
                ReplaceEdit(jetElement.getTextDocumentOffset(document), textLength, newName))
    }
    
    private fun obtainOriginElement(javaElement: IJavaElement): IJavaElement {
        return when (javaElement) {
            is KotlinLightType -> javaElement.originElement
            is KotlinLightFunction -> javaElement.originMethod
            else -> javaElement
        }
    }
}

data class FileEdit(val file: IFile, val edit: TextEdit)