package org.jetbrains.kotlin.core.model

import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.core.JavaModelException
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetClass

public object KotlinJavaManager {
	public val KOTLIN_BIN_FOLDER: Path = Path("kotlin_bin")
    
    public fun getKotlinBinFolderFor(project: IProject): IFolder = project.getFolder(KOTLIN_BIN_FOLDER)
    
    public fun findEclipseType(jetClass: JetClass, javaProject: IJavaProject): IType? {
        return jetClass.getFqName().let {
            if (it != null) javaProject.findType(it.asString()) else null
        }
    }
    
    public fun hasLinkedKotlinBinFolder(javaProject: IJavaProject): Boolean {
        val folder = javaProject.getProject().getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER)
        return folder.isLinked() && KotlinFileSystem.SCHEME == folder.getLocationURI().getScheme()
    }
}