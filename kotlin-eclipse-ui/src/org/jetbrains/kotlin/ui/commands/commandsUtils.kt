package org.jetbrains.kotlin.ui.commands

import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jdt.core.IJavaProject

fun getFirstOrNullJavaProject(selection: IStructuredSelection): IJavaProject? {
	return selection.toArray().firstOrNull() as? IJavaProject
}