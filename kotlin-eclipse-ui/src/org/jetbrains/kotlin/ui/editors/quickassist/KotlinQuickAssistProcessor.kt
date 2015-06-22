package org.jetbrains.kotlin.ui.editors.quickassist

import org.eclipse.core.runtime.CoreException
import org.eclipse.jdt.ui.text.java.IInvocationContext
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal
import org.eclipse.jdt.ui.text.java.IProblemLocation
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor
import com.google.common.collect.Lists
import java.util.ArrayList

public class KotlinQuickAssistProcessor : IQuickAssistProcessor {
    override public fun hasAssists(context: IInvocationContext) : Boolean = getAssists(context, null).isNotEmpty()
    
    override public fun getAssists(context: IInvocationContext?, locations: Array<IProblemLocation>?) : Array<IJavaCompletionProposal> {
        val allApplicableProposals = ArrayList<IJavaCompletionProposal>()
        
        getSingleKotlinQuickAssistProposals().filterTo(allApplicableProposals) { it.isApplicable() }
        
        getKotlinQuickAssistProposalsGenerators().filter { 
            it.isApplicable() 
        }.forEach { allApplicableProposals.addAll(it.getProposals()) }
        
        return allApplicableProposals.toTypedArray()
    }
    
    private fun getSingleKotlinQuickAssistProposals() : List<KotlinQuickAssistProposal> {
        return listOf(
        	KotlinReplaceGetAssistProposal(), 
        	KotlinSpecifyTypeAssistProposal(),
        	KotlinImplementMethodsProposal())
    }
    
    private fun getKotlinQuickAssistProposalsGenerators() : List<KotlinQuickAssistProposalsGenerator> {
        return listOf(KotlinAutoImportProposalsGenerator())
    }
}