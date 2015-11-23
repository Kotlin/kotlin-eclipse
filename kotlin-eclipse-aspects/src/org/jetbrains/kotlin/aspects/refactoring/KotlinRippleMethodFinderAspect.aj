package org.jetbrains.kotlin.aspects.refactoring;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.internal.corext.refactoring.base.ReferencesInBinaryContext;
import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder2;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;

@SuppressWarnings("restriction")
public aspect KotlinRippleMethodFinderAspect {
    pointcut getRelatedMethods(IMethod method, ReferencesInBinaryContext binaryRefs, IProgressMonitor pm, WorkingCopyOwner owner) :
        args(method, binaryRefs, pm, owner)
        && execution(IMethod[] RippleMethodFinder2.getRelatedMethods(IMethod, ReferencesInBinaryContext, 
                IProgressMonitor, WorkingCopyOwner));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    IMethod[] around(IMethod method, ReferencesInBinaryContext binaryRefs, IProgressMonitor pm, WorkingCopyOwner owner) : 
            getRelatedMethods(method, binaryRefs, pm, owner) {
        if (EclipseJavaElementUtil.isKotlinLightClass(method)) {
            return new IMethod[] { method };
        }
        
        return proceed(method, binaryRefs, pm, owner);
    }
}
