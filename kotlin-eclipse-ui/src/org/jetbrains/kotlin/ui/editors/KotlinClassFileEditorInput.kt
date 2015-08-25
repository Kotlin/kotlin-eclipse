package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.core.IBuffer
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.internal.core.BufferManager
import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput

public class KotlinClassFileEditorInput(classFile:IClassFile, private val javaProject:IJavaProject?):InternalClassFileEditorInput(classFile) {

    private fun compareSources(another:KotlinClassFileEditorInput):Boolean {
        try {
            return getClassFile().getSource() == another.getClassFile().getSource()
        }
        catch (e:JavaModelException) {
            return false
        }
    }

    override public fun equals(other: Any?):Boolean =
        when {
            super.equals(other) -> true
            other is KotlinClassFileEditorInput -> compareSources(other)
            else -> false
        }
}