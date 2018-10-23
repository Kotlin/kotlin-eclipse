package org.jetbrains.kotlin.core

import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.ClasspathContainerInitializer
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem
import org.jetbrains.kotlin.core.model.KotlinJavaManager
import java.net.URI

class KotlinClasspathContainerInitializer : ClasspathContainerInitializer() {
    override fun initialize(containerPath: IPath, javaProject: IJavaProject) {
        if (JavaCore.getClasspathContainer(runtimeContainerId, javaProject) !is KotlinClasspathContainer) {
            if (!KotlinJavaManager.hasLinkedKotlinBinFolder(javaProject.project)) {
	    		addFolderForKotlinClassFiles(javaProject)
	    	}
            
        	JavaCore.setClasspathContainer(containerPath, arrayOf(javaProject), arrayOf(KotlinClasspathContainer(javaProject)), null)
        }
    }
}

private fun setKotlinFileSystemScheme(folder: IFolder) : URI {
    val locationURI = folder.locationURI
    val path = Path(folder.project.name).append(KotlinJavaManager.KOTLIN_BIN_FOLDER).makeAbsolute()
    return URI(
    		KotlinFileSystem.SCHEME, 
    		locationURI.userInfo,
    		locationURI.host,
	        locationURI.port,
	        path.toPortableString(), 
	        locationURI.query,
	        locationURI.fragment)
}
    
private fun addFolderForKotlinClassFiles(javaProject: IJavaProject) {
    val folder = javaProject.project.getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER)
    folder.createLink(setKotlinFileSystemScheme(folder), IResource.REPLACE or IResource.ALLOW_MISSING_LOCAL, null)
}