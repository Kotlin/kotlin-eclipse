package org.jetbrains.kotlin.ui.editors.quickfix

import org.eclipse.ui.IMarkerResolutionGenerator2
import org.eclipse.ui.IMarkerResolutionGenerator
import org.eclipse.core.resources.IMarker
import org.eclipse.ui.IMarkerResolution
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager.MARKER_PROBLEM_TYPE
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager.CAN_FIX_PROBLEM
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.ui.editors.quickassist.getBindingContext
import org.jetbrains.kotlin.eclipse.ui.utils.findElementByDocumentOffset
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil

class KotlinMarkerResolutionGenerator : IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {
    override fun getResolutions(marker: IMarker): Array<IMarkerResolution> {
        val diagnostics = obtainDiagnostics(marker)
        val resolutions = arrayListOf<IMarkerResolution>()
        for (quickFix in kotlinQuickFixes) {
            val fixableDiagnostic = diagnostics.find { quickFix.canFix(it) }
            if (fixableDiagnostic == null) continue
            
            when (quickFix) {
                is KotlinDiagnosticQuickFix -> resolutions.add(quickFix.getResolution(fixableDiagnostic))
                is KotlinDiagnosticQuickFixGenerator -> resolutions.addAll(quickFix.getResolutions(fixableDiagnostic))
            }
        }
        
        return resolutions.toTypedArray()
    }
    
    override fun hasResolutions(marker: IMarker): Boolean {
        return marker.type == MARKER_PROBLEM_TYPE && marker.getAttribute(CAN_FIX_PROBLEM, false)
    }
}

private fun obtainDiagnostics(marker: IMarker): List<Diagnostic> {
    val resource = marker.getResource()
    if (resource !is IFile) return emptyList()
    
    val ktFile = KotlinPsiManager.INSTANCE.getParsedFile(resource)
    
    val javaProject = JavaCore.create(resource.project)
    val bindingContext = getBindingContext(ktFile, javaProject)
    if (bindingContext == null) return emptyList()
    
    val document = EditorUtil.getDocument(resource)
    val offset = LineEndUtil.convertCrToDocumentOffset(document, marker.getAttribute(IMarker.CHAR_START, -1))
    
    return bindingContext.diagnostics.filter { diagnostic ->
        diagnostic.textRanges.any { range ->
            range.startOffset <= offset && offset <= range.endOffset
        }
    }
}