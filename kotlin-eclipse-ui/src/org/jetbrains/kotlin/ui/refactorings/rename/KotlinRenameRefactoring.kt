package org.jetbrains.kotlin.ui.refactorings.rename

import org.jetbrains.kotlin.psi.JetElement
import org.eclipse.ltk.core.refactoring.Refactoring
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.Change
import org.eclipse.ltk.core.refactoring.participants.RenameArguments

public class KotlinRenameRefactoring(val jetElement: JetElement, val newName: String) : Refactoring() {
    val renameParticipant = KotlinLocalPropertyRenameParticipant()
    
    override fun checkFinalConditions(pm: IProgressMonitor?): RefactoringStatus? {
        renameParticipant.checkConditions(null, null)
        return RefactoringStatus()
    }
    
    override fun checkInitialConditions(pm: IProgressMonitor?): RefactoringStatus? {
        renameParticipant.initialize(null, jetElement, RenameArguments(newName, true))
        return RefactoringStatus()
    }
    
    override fun getName(): String = "Kotlin Local Element Rename Refactoring"
    
    override fun createChange(pm: IProgressMonitor?): Change? {
        return renameParticipant.createChange(null)
    }
}