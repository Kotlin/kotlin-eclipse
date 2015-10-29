package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.jetbrains.kotlin.psi.KtFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.text.IDocument

public interface KotlinEditor {
	val javaEditor: JavaEditor
	val parsedFile: KtFile?
	val javaProject: IJavaProject?
	val document: IDocument
	fun isEditable(): Boolean
}