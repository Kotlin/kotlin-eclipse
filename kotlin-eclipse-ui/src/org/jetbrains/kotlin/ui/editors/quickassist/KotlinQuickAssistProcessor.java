package org.jetbrains.kotlin.ui.editors.quickassist;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

import com.google.common.collect.Lists;

public class KotlinQuickAssistProcessor implements IQuickAssistProcessor {

    @Override
    public boolean hasAssists(IInvocationContext context) throws CoreException {
        return getAssists(context, null).length > 0;
    }

    @Override
    public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
            throws CoreException {
        List<KotlinQuickAssistProposal> allProposals = getSingleKotlinQuickAssistProposals();
        
        List<IJavaCompletionProposal> applicableProposals = Lists.newArrayList();
        for (KotlinQuickAssistProposal proposal : allProposals) {
            if (proposal.isApplicable()) {
                applicableProposals.add(proposal);
            }
        }
        
        for (KotlinQuickAssistProposalsGenerator proposalGenerator: getKotlinQuickAssistProposalsGenerators()) {
            if (proposalGenerator.isApplicable()) {
                applicableProposals.addAll(proposalGenerator.getProposals());
            }
        }
        
        return applicableProposals.toArray(new IJavaCompletionProposal[applicableProposals.size()]);
    }
    
    private List<KotlinQuickAssistProposal> getSingleKotlinQuickAssistProposals() {
        List<KotlinQuickAssistProposal> proposals = Lists.newArrayList();
        
        proposals.add(new KotlinReplaceGetAssistProposal());
        proposals.add(new KotlinSpecifyTypeAssistProposal());
        
        return proposals;
    }
    
    
    private List<KotlinQuickAssistProposalsGenerator> getKotlinQuickAssistProposalsGenerators() {
        List<KotlinQuickAssistProposalsGenerator> proposalsGenerators = Lists.newArrayList();
        
        proposalsGenerators.add(new KotlinAutoImportProposalsGenerator());
        
        return proposalsGenerators;
    }
}