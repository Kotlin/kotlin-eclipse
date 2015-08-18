package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.jetbrains.kotlin.psi.JetFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.text.IDocument

public interface KotlinEditor {
	val javaEditor: JavaEditor
	val parsedFile: JetFile?
	val javaProject: IJavaProject?
	val document: IDocument
	fun isEditable(): Boolean
}