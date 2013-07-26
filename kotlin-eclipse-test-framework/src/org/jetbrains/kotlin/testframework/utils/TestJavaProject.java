package org.jetbrains.kotlin.testframework.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.core.IPackageFragment;
import org.jetbrains.kotlin.model.KotlinNature;

public class TestJavaProject {
	
	private final static String SRC_FOLDER = "src";
	
	private IProject project;
	private IJavaProject javaProject;
	
	private IPackageFragmentRoot sourceFolder;
	
	public TestJavaProject(String projectName) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		
		project = workspaceRoot.getProject(projectName);
		try {
			boolean projectExists = project.exists();
			if (!projectExists) {
				project.create(null);
				project.open(null);
			}
			
			javaProject = JavaCore.create(project);
			
			if (!projectExists) {
				setNatureAndBuilder();
				
				javaProject.setRawClasspath(new IClasspathEntry[0], null);
				
				sourceFolder = createSourceFolder();
				
				addSystemLibraries();
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void setNatureAndBuilder() throws CoreException {
        IProjectDescription description = project.getDescription();
        description.setNatureIds(new String[] { JavaCore.NATURE_ID });
        project.setDescription(description, null);
        
        KotlinNature.addNature(project);
        KotlinNature.addBuilder(project);
    }
	
	public IFile createFile(IContainer folder, String name, InputStream content) {
		IFile file = folder.getFile(new Path(name));
		try {
			if (file.exists()) {
				file.delete(true,  null);
			}
			file.create(content, true, null);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		
		return file;
	}
	
	public IFile createSourceFile(String pkg, String fileName, String content) throws CoreException {
		IPackageFragment fragment = createPackage(pkg);
		return createFile((IFolder) fragment.getResource(), fileName, new ByteArrayInputStream(content.getBytes()));
	}

	public IPackageFragment createPackage(String name) throws CoreException {
		if (sourceFolder == null) {
			sourceFolder = createSourceFolder();
		}
		return sourceFolder.createPackageFragment(name, true, null);
	}
	
	private IPackageFragmentRoot createSourceFolder() throws CoreException {
		IFolder folder = createFolderIfNotExist(SRC_FOLDER);
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);
		for (IClasspathEntry entry : javaProject.getResolvedClasspath(false)) {
			if (folder.getFullPath().equals(entry.getPath())) {
				return root;
			}
		}
		
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
        newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath());
        javaProject.setRawClasspath(newEntries, null);
        
        return root;
	}
	
	public IFolder createFolderIfNotExist(String name) throws CoreException {
		IFolder folder = project.getFolder(name);
		if (!folder.exists()) {
			folder.create(false, true, null);
		}
		
		return folder;
	}
	
	private void addSystemLibraries() throws JavaModelException {
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
        
        System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
        
        newEntries[oldEntries.length] = JavaRuntime.getDefaultJREContainerEntry();
        
        javaProject.setRawClasspath(newEntries, null);
	}
}
