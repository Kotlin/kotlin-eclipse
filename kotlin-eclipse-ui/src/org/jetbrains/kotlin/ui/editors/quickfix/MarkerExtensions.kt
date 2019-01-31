package org.jetbrains.kotlin.ui.editors.quickfix

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.jetbrains.kotlin.diagnostics.Diagnostic

private const val ANNOTATION_DIAGNOSTIC_HASH = "annotationDiagnostic"

private val resourceDiagnosticsMapping = hashMapOf<IFile, HashMap<Int, Diagnostic>>()

val IMarker.diagnostic: Diagnostic?
    get() = annotationCode?.let { code ->
        (resource as? IFile)?.let { file ->
            resourceDiagnosticsMapping[file]?.get(code)
        }
    }

private var IMarker.annotationCode: Int?
    get() = getAttribute(ANNOTATION_DIAGNOSTIC_HASH) as? Int
    set(value) = setAttribute(ANNOTATION_DIAGNOSTIC_HASH, value)

internal fun IMarker.addDiagnostics(diagnostic: Diagnostic) {
    val hashCode = diagnostic.hashCode()
    resourceDiagnosticsMapping.getOrPut(resource as IFile) { hashMapOf() }[hashCode] = diagnostic
    annotationCode = hashCode
}

internal fun IFile.removeMarkers() {
    deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)
    resourceDiagnosticsMapping.remove(this)
}

