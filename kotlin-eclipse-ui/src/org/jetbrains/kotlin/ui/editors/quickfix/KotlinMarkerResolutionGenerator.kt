package org.jetbrains.kotlin.ui.editors.quickfix

import org.eclipse.ui.IMarkerResolutionGenerator2
import org.eclipse.ui.IMarkerResolutionGenerator
import org.eclipse.core.resources.IMarker
import org.eclipse.ui.IMarkerResolution

class KotlinMarkerResolutionGenerator : IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {
    override fun getResolutions(marker: IMarker): Array<IMarkerResolution> {
        return emptyArray()
    }
    
    override fun hasResolutions(marker: IMarker): Boolean {
        return true
    }
}