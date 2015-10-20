package org.jetbrains.kotlin.ui.editors.markoccurrences

import org.eclipse.ui.ISelectionListener
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IWorkbenchPart
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.jface.text.ITextSelection
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.ui.search.KotlinQueryParticipant
import org.eclipse.search.ui.text.Match
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.jdt.core.search.IJavaSearchConstants

public class KotlinMarkOccurrencesFinder(val editor: KotlinFileEditor) : ISelectionListener {
    override fun selectionChanged(part: IWorkbenchPart, selection: ISelection) {
        if (part is KotlinFileEditor && selection is ITextSelection) {
            val jetElement = EditorUtil.getJetElement(part, selection.getOffset())
            if (jetElement == null) return
            
            KotlinPsiManager.getKotlinFileIfExist(editor.getFile()!!, editor.document.get())
            
            findOccurrencesInFile(jetElement, part.parsedFile!!)
        }
        throw UnsupportedOperationException()
    }
    
    private fun findOccurrencesInFile(jetElement: JetElement, jetFile: JetFile) {
        val kotlinQueryParticipant = KotlinQueryParticipant()
        val matches = arrayListOf<Match>()
        
        val factory = JavaSearchScopeFactory.getInstance()
//        val querySpecification = KotlinQueryPatternSpecification(
//                jetElement,
//                jetElement.getContainingJetFile(),
//                IJavaSearchConstants.ALL_OCCURRENCES,
//                factory.createWorkspaceScope(false), 
//                factory.getWorkspaceScopeDescription(false))
        
    }
}