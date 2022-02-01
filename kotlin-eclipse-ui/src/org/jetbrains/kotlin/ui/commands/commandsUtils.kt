package org.jetbrains.kotlin.ui.commands

import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.internal.core.CompilationUnit
import org.eclipse.ui.ide.undo.DeleteResourcesOperation
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IFile
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.ide.IDE
import org.eclipse.jdt.core.JavaCore

data class ConvertedKotlinData(val file: IFile, val kotlinFileData: String)

fun getFirstOrNullProject(selection: IStructuredSelection): IProject? {
	return selection.toArray().firstOrNull().let { if( it is IJavaProject) it.getProject() else it as? IProject }
}

fun getDeleteOperation(compilationUnits: Set<CompilationUnit>): DeleteResourcesOperation {
	val resources = compilationUnits.map { it.getResource() }
	return DeleteResourcesOperation(resources.toTypedArray(), "Deleting files", true)
}

fun getCorrespondingProjects(compilationUnits: Set<CompilationUnit>): Set<IProject> {
	return compilationUnits.map { 
		it.getJavaProject().getProject() 
	}.toSet() 
}

fun openEditor(file: IFile) {
	IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
}