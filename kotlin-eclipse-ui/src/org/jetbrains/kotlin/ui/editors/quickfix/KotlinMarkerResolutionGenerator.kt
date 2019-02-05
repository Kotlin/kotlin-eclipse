package org.jetbrains.kotlin.ui.editors.quickfix

import org.eclipse.core.resources.IMarker
import org.eclipse.ui.IMarkerResolution
import org.eclipse.ui.IMarkerResolutionGenerator
import org.eclipse.ui.IMarkerResolutionGenerator2
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager.CAN_FIX_PROBLEM
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager.MARKER_PROBLEM_TYPE

class KotlinMarkerResolutionGenerator : IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

    override fun getResolutions(marker: IMarker): Array<IMarkerResolution> =
        getResolutions(listOfNotNull(marker.diagnostic)).toTypedArray()

    override fun hasResolutions(marker: IMarker): Boolean =
        marker.type == MARKER_PROBLEM_TYPE && marker.getAttribute(CAN_FIX_PROBLEM, false)

    companion object {
        fun getResolutions(diagnostics: List<Diagnostic>): List<KotlinMarkerResolution> =
            diagnostics.flatMap { getResolutions(it) }

        fun getResolutions(diagnostic: Diagnostic): List<KotlinMarkerResolution> =
            kotlinQuickFixes.getOrDefault(diagnostic.factory, mutableListOf()).flatMap { quickFix ->
                (quickFix as? KotlinDiagnosticQuickFix)?.getResolutions(diagnostic) ?: listOf()
            }
    }
}