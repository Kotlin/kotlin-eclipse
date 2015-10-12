package org.jetbrains.kotlin.ui.refactorings.rename

import org.eclipse.ltk.core.refactoring.participants.RenameProcessor
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext
import org.eclipse.ltk.core.refactoring.Change
import org.jetbrains.kotlin.psi.JetDeclaration
import org.eclipse.ltk.core.refactoring.participants.RenameArguments
import org.eclipse.jdt.internal.corext.refactoring.tagging.INameUpdating
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors
import org.jetbrains.kotlin.core.builder.KotlinPsiManager

public class KotlinRenameProcessor(val jetDeclaration: JetDeclaration, val newName: String) : RenameProcessor() {
    val project = KotlinPsiManager.getJavaProject(jetDeclaration)
    
    override fun isApplicable(): Boolean = true
    
    override fun loadParticipants(status: RefactoringStatus?, sharedParticipants: SharableParticipants?): Array<out RefactoringParticipant> {
        return ParticipantManager.loadRenameParticipants(
                status, 
                this, 
                jetDeclaration, 
                RenameArguments(newName, true), 
                JavaProcessors.computeAffectedNatures(project),
                sharedParticipants)
    }
    
    override fun checkFinalConditions(pm: IProgressMonitor?, context: CheckConditionsContext?): RefactoringStatus? {
        return RefactoringStatus()
    }
    
    override fun checkInitialConditions(pm: IProgressMonitor?): RefactoringStatus? {
        return RefactoringStatus()
    }
    
    override fun getIdentifier(): String = "org.jetbrains.kotlin.ui.refactoring.renameProcessor"
    
    override fun getProcessorName(): String = "Kotlin Rename Processor"
    
    override fun createChange(pm: IProgressMonitor?): Change? = null
    
    override fun getElements(): Array<Any> = arrayOf(jetDeclaration)
}