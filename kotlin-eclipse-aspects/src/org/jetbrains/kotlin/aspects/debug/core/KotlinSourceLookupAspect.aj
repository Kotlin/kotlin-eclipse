/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.aspects.debug.core;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.debug.KotlinSourceLookupNavigator;
import org.jetbrains.kotlin.idea.JetFileType;

public aspect KotlinSourceLookupAspect {

	pointcut getSourceName(Object object) : 
				args(object) 
				&& execution(String JavaSourceLookupParticipant.getSourceName(Object));

	@SuppressAjWarnings({"adviceDidNotMatch"})
	String around(Object object) throws CoreException : getSourceName(object) {
		String sourcePath = proceed(object);
		if (sourcePath.endsWith(JetFileType.INSTANCE.getDefaultExtension())) {
			IJavaStackFrame frame = getStackFrame(object);
			IPath kotlinSourcePath = KotlinSourceLookupNavigator.INSTANCE.findKotlinSourceFile(frame);
			
			return kotlinSourcePath != null ? kotlinSourcePath.toOSString() : sourcePath;
		}
		
		return sourcePath;
	}
	
	@Nullable
	private IJavaStackFrame getStackFrame(Object stackFrame) {
		if (stackFrame instanceof IAdaptable) {
			return (IJavaStackFrame) ((IAdaptable) stackFrame).getAdapter(IJavaStackFrame.class);
		}
		
		return null;
	}
}