package org.jetbrains.kotlin.core.model

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import com.intellij.psi.PsiJavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.name.FqName

class EclipseKotlinJavaModuleResolver : JavaModuleResolver {
    override fun checkAccessibility(
            fileFromOurModule: VirtualFile?,
            referencedFile: VirtualFile,
            referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? = null
}