package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor
import org.jetbrains.kotlin.psi.KtFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.text.IDocument
import org.eclipse.core.resources.IFile

public interface KotlinEditor {
    val javaEditor: JavaEditor
    val parsedFile: KtFile?
    val javaProject: IJavaProject?
    val document: IDocument
    val eclipseFile: IFile?
    fun isEditable(): Boolean
}