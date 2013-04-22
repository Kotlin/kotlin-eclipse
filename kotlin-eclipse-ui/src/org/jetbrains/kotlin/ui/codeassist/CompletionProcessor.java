package org.jetbrains.kotlin.ui.codeassist;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.jetbrains.kotlin.ui.editors.Scanner;

public class CompletionProcessor implements IContentAssistProcessor{
    
    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
        String keyWords[] = Scanner.getKeyWords();        
        
        ICompletionProposal[] result= new ICompletionProposal[keyWords.length];
        for (int i= 0; i < keyWords.length; i++) {
            IContextInformation info= new ContextInformation(keyWords[i], keyWords[i]);
            result[i]= new CompletionProposal(keyWords[i], documentOffset, 0, keyWords[i].length(), null, keyWords[i], info, keyWords[i]);
        }
        
        return result;
    }
    
    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] { '.' };
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
        return null;
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }
}
