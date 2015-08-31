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

    private fun compareSources(another:KotlinClassFileEditorInput):Boolean {
        try {
            val sourceShortName = getSourceShortName() ?: return false
            val anotherSourceShortName = another.getSourceShortName() ?: return false
            return when {
                sourceShortName != anotherSourceShortName -> false
                getClassFile().getParent() == another.getClassFile().getParent() -> true
                else -> resolveShortSourceName(sourceShortName) == another.resolveShortSourceName(anotherSourceShortName)
            }
        } catch (e:JavaModelException) {
            return false
        }
    }

    override public fun equals(other: Any?):Boolean =
            when {
                super.equals(other) -> true
                other is KotlinClassFileEditorInput -> compareSources(other)
                else -> false
            }

    private fun getSourceShortName() =
            (getClassFile().getType() as? BinaryType)?.getSourceFileName(null)?.toString()

    private fun resolveShortSourceName(simpleSourceFileName: String): String {
        val index = KotlinSourceIndex.getInstance(javaProject);
        val packageFragment = getClassFile().getParent() as PackageFragment;
        return index.resolvePath(packageFragment, simpleSourceFileName);
    }
}