package org.jetbrains.kotlin.ui.editors.highlighting

import org.eclipse.compare.IViewerCreator
import org.eclipse.jface.viewers.Viewer
import org.eclipse.compare.CompareConfiguration
import org.eclipse.swt.widgets.Composite
import org.eclipse.compare.contentmergeviewer.TextMergeViewer
import org.eclipse.jface.text.TextViewer
import org.eclipse.jface.text.source.SourceViewer
import org.eclipse.jface.text.source.SourceViewerConfiguration
import org.eclipse.jface.text.presentation.IPresentationReconciler
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.ui.IStorageEditorInput
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinTokenScanner
import org.eclipse.jface.text.presentation.PresentationReconciler
import org.eclipse.jface.text.rules.DefaultDamagerRepairer
import org.eclipse.jface.text.IDocument
import org.eclipse.jdt.internal.ui.text.JavaColorManager
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.ui.editors.Configuration


public class KotlinViewerCreator : IViewerCreator {
	override fun createViewer(parent: Composite, configuration: CompareConfiguration): Viewer {
		return KotlinMergeViewer(parent, configuration)
	}
}

class KotlinMergeViewer(parent: Composite, configuration: CompareConfiguration): TextMergeViewer(parent, configuration) {
	private val colorManager = JavaColorManager()
	
	override fun configureTextViewer(viewer: TextViewer) {
		if (viewer !is SourceViewer) return
		
		val configuration = object : SourceViewerConfiguration() {
			override fun getPresentationReconciler(sourceViewer: ISourceViewer): IPresentationReconciler? {
				val scanner = KotlinTokenScanner.createScannerForCompareViewOfKtSourceFile(
                        JavaPlugin.getDefault().getPreferenceStore(), colorManager)
				return Configuration.getKotlinPresentaionReconciler(scanner)
			}
		}
		
		viewer.configure(configuration)
	}
}