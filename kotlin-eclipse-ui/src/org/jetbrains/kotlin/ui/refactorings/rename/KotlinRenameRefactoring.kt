package org.jetbrains.kotlin.ui.refactorings.rename

import org.eclipse.ltk.core.refactoring.participants.RenameParticipant
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.Change

public class KotlinRenameRefactoring : RenameParticipant() {
    override fun initialize(element: Any?): Boolean {
        return true
    }
    
    override fun checkConditions(pm: IProgressMonitor?, context: CheckConditionsContext?): RefactoringStatus? {
        return null
    }
    
    override fun getName(): String? {
        return null
    }
    
    override fun createChange(pm: IProgressMonitor?): Change? {
        return null
    }
}