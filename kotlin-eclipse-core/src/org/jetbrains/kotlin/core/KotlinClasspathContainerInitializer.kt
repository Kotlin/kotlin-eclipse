package org.jetbrains.kotlin.core

import java.net.URI
import java.net.URISyntaxException
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.ClasspathContainerInitializer
import org.eclipse.jdt.core.IClasspathContainer
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.filesystem.KotlinFileSystem
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinJavaManager

public class KotlinClasspathContainerInitializer : ClasspathContainerInitializer() {
    override public fun initialize(containerPath: IPath, javaProject: IJavaProject) {
        if (!(JavaCore.getClasspathContainer(runtimeContainerId, javaProject) is KotlinClasspathContainer)) {
            if (!KotlinJavaManager.INSTANCE.hasLinkedKotlinBinFolder(javaProject)) {
	    		addFolderForKotlinClassFiles(javaProject)
	    	}
            
        	JavaCore.setClasspathContainer(containerPath, arrayOf(javaProject), arrayOf(KotlinClasspathContainer(javaProject)), null)
        }
    }
}

private fun setKotlinFileSystemScheme(folder: IFolder) : URI {
    val locationURI = folder.getLocationURI()
    val path = Path(folder.getProject().getName()).append(KotlinJavaManager.KOTLIN_BIN_FOLDER).makeAbsolute()
    return URI(
    		KotlinFileSystem.SCHEME, 
    		locationURI.getUserInfo(), 
    		locationURI.getHost(), 
	        locationURI.getPort(), 
	        path.toPortableString(), 
	        locationURI.getQuery(), 
	        locationURI.getFragment())
}
    
private fun addFolderForKotlinClassFiles(javaProject: IJavaProject) {
    val folder = javaProject.getProject().getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER)
    folder.createLink(setKotlinFileSystemScheme(folder), IResource.REPLACE or IResource.ALLOW_MISSING_LOCAL, null)
}