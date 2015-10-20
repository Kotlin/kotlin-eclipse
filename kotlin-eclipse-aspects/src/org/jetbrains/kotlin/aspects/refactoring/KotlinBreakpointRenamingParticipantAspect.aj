package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.debug.core.refactoring.BreakpointChange;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;

@SuppressWarnings("restriction")
public aspect KotlinBreakpointRenamingParticipantAspect {
    pointcut findElement(IJavaElement parent, IJavaElement element): 
                args(parent, element) 
                && execution(IJavaElement BreakpointChange.findElement(IJavaElement, IJavaElement));

    // BreakpointRenameParticipant operates with compilation unit to rename breakpoint configuration
    // Thus we disable it for Kotlin files
    @SuppressAjWarnings({"adviceDidNotMatch"})
    IJavaElement around(IJavaElement parent, IJavaElement element): findElement(parent, element) {
        IResource resource = parent.getResource();
        if (resource instanceof IFile && KotlinPsiManager.isKotlinFile((IFile) resource)) {
            return null;
        }
        
        return proceed(parent, element);
    }
}