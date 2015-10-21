package org.jetbrains.kotlin.ui.refactorings.extract

import org.eclipse.ltk.core.refactoring.Refactoring
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ltk.core.refactoring.RefactoringStatus
import org.eclipse.ltk.core.refactoring.Change
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages
import org.jetbrains.kotlin.psi.JetExpression
import com.intellij.psi.util.PsiTreeUtil

public class KotlinExtractVariableRefactoring(val expression: JetExpression) : Refactoring() {
    public var newName: String = "temp"
    
    override fun checkFinalConditions(pm: IProgressMonitor?): RefactoringStatus = RefactoringStatus()
    
    override fun checkInitialConditions(pm: IProgressMonitor?): RefactoringStatus? = RefactoringStatus()
    
    override fun getName(): String = RefactoringCoreMessages.ExtractTempRefactoring_name
    
    override fun createChange(pm: IProgressMonitor?): Change? {
        
        return null
    }
    
    private fun introduceVariable() {
        val commonParent = PsiTreeUtil.findCommonParent(listOf(expression))
        
    }
}