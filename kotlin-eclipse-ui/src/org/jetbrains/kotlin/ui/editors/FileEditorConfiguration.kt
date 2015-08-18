package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.quickassist.QuickAssistAssistant
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.text.reconciler.IReconciler
import org.eclipse.jface.text.reconciler.MonoReconciler
import org.eclipse.jface.text.IAutoEditStrategy
import org.eclipse.jface.text.contentassist.IContentAssistant
import org.eclipse.jface.text.contentassist.ContentAssistant
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProcessor
import org.eclipse.jface.text.IDocument

public class FileEditorConfiguration(colorManager: IColorManager,
        private val fileEditor: KotlinFileEditor,
        preferenceStore: IPreferenceStore): Configuration(colorManager, fileEditor, preferenceStore) {

    override fun getQuickAssistAssistant(sourceViewer: ISourceViewer): IQuickAssistAssistant? {
        val quickAssist = QuickAssistAssistant()
        quickAssist.setQuickAssistProcessor(KotlinCorrectionProcessor(fileEditor))
        quickAssist.setInformationControlCreator(getInformationControlCreator(sourceViewer))
        return quickAssist
    }

    override fun getReconciler(sourceViewer: ISourceViewer) =
            MonoReconciler(KotlinReconcilingStrategy(fileEditor), false)

    override fun getAutoEditStrategies(sourceViewer: ISourceViewer, contentType: String) =
            arrayOf(KotlinAutoIndentStrategy(fileEditor))

    override fun getContentAssistant(sourceViewer: ISourceViewer): IContentAssistant? {
        val assistant = ContentAssistant()
        val completionProcessor = KotlinCompletionProcessor(fileEditor)

        assistant.setContentAssistProcessor(completionProcessor, IDocument.DEFAULT_CONTENT_TYPE)
        assistant.addCompletionListener(completionProcessor)
        assistant.enableAutoActivation(true)
        assistant.setAutoActivationDelay(500)
        assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY)
        assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE)

        return assistant
    }
}