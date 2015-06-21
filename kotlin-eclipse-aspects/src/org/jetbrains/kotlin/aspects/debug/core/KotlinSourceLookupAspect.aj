package org.jetbrains.kotlin.aspects.debug.core;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.jetbrains.kotlin.core.debug.KotlinSourceLookupNavigator;

public aspect KotlinSourceLookupAspect {
	pointcut getSourceElement(Object object) : 
		args(object) 
		&& execution(Object ISourceLookupDirector.getSourceElement(Object));
	
	@SuppressAjWarnings({"adviceDidNotMatch"})
	Object around(Object object) : getSourceElement(object) {
		Object result = proceed(object);
		if (result instanceof IFile) {
			String fileName = ((IFile) result).getName();
			if (JavaCore.isJavaLikeFileName(fileName)) {
				return result;
			}
		}
		
		if (object instanceof IJavaStackFrame) {
			IJavaStackFrame frame = (IJavaStackFrame) object;
			IFile kotlinSourceFile = KotlinSourceLookupNavigator.INSTANCE.findKotlinSourceFile(frame);
			
			return kotlinSourceFile != null ? kotlinSourceFile : result;
		}
		
		return result;
	}
}
