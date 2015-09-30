package org.jetbrains.kotlin.ui

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider
import org.eclipse.jdt.core.ICompilationUnit

public class KotlinDocumentProvider private constructor() : CompilationUnitDocumentProvider() {
    companion object {
        val provider by lazy { KotlinDocumentProvider() }
    }
    
    override fun getWorkingCopy(element: Any?): ICompilationUnit? {
        return super.getWorkingCopy(element)
    }
}