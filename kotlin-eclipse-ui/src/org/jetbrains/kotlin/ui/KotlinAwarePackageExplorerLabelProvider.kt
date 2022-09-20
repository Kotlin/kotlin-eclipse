package org.jetbrains.kotlin.ui

import org.eclipse.core.resources.IFile
import org.eclipse.jdt.core.IPackageFragment
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerLabelProvider
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider
import org.eclipse.swt.graphics.Image
import org.jetbrains.kotlin.core.model.EclipseScriptDefinitionProvider
import org.jetbrains.kotlin.core.utils.asFile
import kotlin.script.experimental.host.FileScriptSource

/**
 * Modified version of [PackageExplorerLabelProvider] that returns correct images for packages
 * that contains only Kotlin source files.
 *
 * Original [PackageExplorerLabelProvider] treats Kotlin source files as non-Java resources
 * and returns "empty" package icon.
 *
 * Injected by [org.jetbrains.kotlin.aspects.ui.PackageExplorerLabelProviderAspect]
 */
class KotlinAwarePackageExplorerLabelProvider(cp: PackageExplorerContentProvider) : PackageExplorerLabelProvider(cp) {

    override fun getImage(element: Any?): Image? {
        // Replace instances of IPackageFragment with instances of KotlinAwarePackageFragment
        return super.getImage(when (element) {
            is IPackageFragment -> KotlinAwarePackageFragment(element)
            else -> element
        })
    }

    class KotlinAwarePackageFragment(private val base: IPackageFragment) : IPackageFragment by base {
        /**
         * Returns true also when a package contains any Kotlin source file.
         *
         * Used by [JavaElementImageProvider.getPackageFragmentIcon]
         */
        override fun hasChildren(): Boolean {
            return base.hasChildren() ||
                    base.nonJavaResources.any { obj ->
                        obj is IFile && (obj.name.endsWith(".kt") || EclipseScriptDefinitionProvider.isScript(FileScriptSource(obj.asFile)))
                    }
        }
    }
}