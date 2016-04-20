package org.jetbrains.kotlin.ui.editors.quickfix

import org.eclipse.core.resources.IMarker
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager.MARKER_PROBLEM_TYPE
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager.CAN_FIX_PROBLEM
import org.eclipse.ui.IMarkerResolution2
import org.eclipse.ui.IMarkerResolution
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.ui.editors.quickassist.getBindingContext
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.eclipse.ui.utils.findElementByDocumentOffset
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

val kotlinQuickFixes: List<KotlinQuickFix> = listOf(KotlinAutoImportQuickFix)

interface KotlinQuickFix {
    // this function must be fast and optimistic
    fun canFix(diagnostic: Diagnostic): Boolean
}

interface KotlinDiagnosticQuickFix : KotlinQuickFix {
    fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution>
}

interface KotlinMarkerResolution: IMarkerResolution, IMarkerResolution2 {
    fun apply(file: IFile)
    
    override fun run(marker: IMarker) {
        val resource = marker.resource
        if (resource is IFile) {
            apply(resource)
        }
    }
}

