package org.jetbrains.kotlin.aspects.debug.ui;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.internal.debug.ui.BreakpointMarkerUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;

public aspect SuppressBreakpointMarkerUpdaterAspect {

	pointcut updateMarker(BreakpointMarkerUpdater markerUpdater, IMarker marker, IDocument document, Position position): 
				args(marker, document, position) 
				&& execution(boolean BreakpointMarkerUpdater.updateMarker(IMarker, IDocument, Position))
				&& target(markerUpdater);

	@SuppressAjWarnings({"adviceDidNotMatch"})
	boolean around(BreakpointMarkerUpdater markerUpdater, IMarker marker, IDocument document, Position position):  
			updateMarker(markerUpdater, marker, document, position) {
		IFile resource = (IFile) marker.getResource();
		if (resource != null && KotlinPsiManager.INSTANCE.exists(resource)) {
			return true;
		}

		return proceed(markerUpdater, marker, document, position);
	}
}