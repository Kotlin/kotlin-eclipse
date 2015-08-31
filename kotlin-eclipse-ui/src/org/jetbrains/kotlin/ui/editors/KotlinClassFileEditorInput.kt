package org.jetbrains.kotlin.ui.editors

import org.eclipse.jdt.core.IBuffer
import org.eclipse.jdt.core.IClassFile
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.internal.core.BufferManager
import org.eclipse.jdt.internal.ui.javaeditor.InternalClassFileEditorInput
import org.eclipse.jdt.internal.compiler.env.IBinaryType
import org.jetbrains.kotlin.core.resolve.KotlinSourceIndex
import org.eclipse.jdt.internal.core.PackageFragment
import org.eclipse.jdt.internal.core.ClassFile
import org.eclipse.jdt.internal.core.BinaryType

public class KotlinClassFileEditorInput(classFile:IClassFile, private val javaProject:IJavaProject?):InternalClassFileEditorInput(classFile) {
    private val sourceShortName: String?
    private val sourceFullPath: String?

    init {
        sourceShortName = (getClassFile().getType() as? BinaryType)?.getSourceFileName(null)?.toString()
        sourceFullPath = if (sourceShortName != null) {
            val index = KotlinSourceIndex.getInstance(javaProject)
            val packageFragment = getClassFile().getParent() as PackageFragment
            index.resolvePath(packageFragment, sourceShortName)
        } else {
            null
        }
    }

    private fun compareSources(another:KotlinClassFileEditorInput):Boolean {
        if (sourceShortName == null || another.sourceShortName == null) {
            return false
        }
        return when {
            sourceShortName != another.sourceShortName -> false
            getClassFile().getParent() == another.getClassFile().getParent() -> true
            else -> sourceFullPath == another.sourceFullPath
        }
    }

    override public fun equals(other: Any?):Boolean =
            when {
                super.equals(other) -> true
                other is KotlinClassFileEditorInput -> compareSources(other)
                else -> false
            }

    override fun getName() =
            sourceShortName ?: super.getName()

    override fun getToolTipText() =
            sourceFullPath ?: super.getToolTipText()
}