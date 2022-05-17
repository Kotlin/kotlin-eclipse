/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.contentassist.ContentAssistant
import org.eclipse.jface.text.contentassist.IContentAssistant
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant
import org.eclipse.jface.text.quickassist.QuickAssistAssistant
import org.eclipse.jface.text.reconciler.IReconciler
import org.eclipse.jface.text.reconciler.MonoReconciler
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.swt.graphics.Color
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinCompletionProcessor
import org.jetbrains.kotlin.ui.editors.codeassist.KotlinContextInfoContentAssistProcessor

class FileEditorConfiguration(colorManager: IColorManager,
        private val fileEditor: KotlinEditor,
        preferenceStore: IPreferenceStore,
        private val reconcilingStrategy: KotlinReconcilingStrategy): Configuration(colorManager, fileEditor, preferenceStore) {
    override fun getQuickAssistAssistant(sourceViewer: ISourceViewer): IQuickAssistAssistant? {
        val quickAssist = QuickAssistAssistant()
        quickAssist.quickAssistProcessor = KotlinCorrectionProcessor(fileEditor)
        quickAssist.setInformationControlCreator(getInformationControlCreator(sourceViewer))
        return quickAssist
    }

    override fun getReconciler(sourceViewer: ISourceViewer): IReconciler {
        return MonoReconciler(reconcilingStrategy, false)
    }

    override fun getAutoEditStrategies(sourceViewer: ISourceViewer, contentType: String) =
            arrayOf(KotlinAutoIndentStrategy(fileEditor))

    override fun getContentAssistant(sourceViewer: ISourceViewer): IContentAssistant = ContentAssistant().apply {
        KotlinCompletionProcessor.createKotlinCompletionProcessors(fileEditor, this).forEach {
            addContentAssistProcessor(it, IDocument.DEFAULT_CONTENT_TYPE)
        }

        addContentAssistProcessor(KotlinContextInfoContentAssistProcessor(fileEditor), IDocument.DEFAULT_CONTENT_TYPE)
        
        val autoActivation = fPreferenceStore.getBoolean(PreferenceConstants.CODEASSIST_AUTOACTIVATION)
        enableAutoActivation(autoActivation)
        
        val delay = fPreferenceStore.getInt(PreferenceConstants.CODEASSIST_AUTOACTIVATION_DELAY)
        autoActivationDelay = delay
        
        val foregroundColor = getColor(fPreferenceStore, PreferenceConstants.CODEASSIST_PARAMETERS_FOREGROUND, colorManager)
        setContextInformationPopupForeground(foregroundColor)
        setContextSelectorForeground(foregroundColor)
        
        val backgroundColor = getColor(fPreferenceStore, PreferenceConstants.CODEASSIST_PARAMETERS_BACKGROUND, colorManager)
        setContextInformationPopupBackground(backgroundColor)
        setContextSelectorBackground(backgroundColor)
        
        val autoInsert = fPreferenceStore.getBoolean(PreferenceConstants.CODEASSIST_AUTOINSERT)
        enableAutoInsert(autoInsert)
        
        setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY)
        setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE)
        
        enableColoredLabels(true)
        
        setShowEmptyList(true)
    }
}

fun getColor(store: IPreferenceStore, key: String, manager: IColorManager): Color {
    val rgb = PreferenceConverter.getColor(store, key)
    return manager.getColor(rgb)
}
