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

package org.jetbrains.kotlin.core.debug;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;

public class KotlinSourceLookupNavigator {
	public static final KotlinSourceLookupNavigator INSTANCE = new KotlinSourceLookupNavigator();
	
	private KotlinSourceLookupNavigator() {
	}
	
//	From JDI model we obtain path to file as "some/pckg/File.kt" and Java seeks file in folder some/pckg what might be wrong
	@Nullable
	public IFile findKotlinSourceFile(@NotNull IJavaStackFrame frame) {
	    ISourceLocator sourceLocator = frame.getLaunch().getSourceLocator();
	    if (!(sourceLocator instanceof ISourceLookupDirector)) {
	    	return null;
	    }
	    
	    try {
            return findKotlinSourceFile(frame, (ISourceLookupDirector) sourceLocator);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
	    
	    return null;
	}

    private IFile findKotlinSourceFile(IJavaStackFrame frame, ISourceLookupDirector lookupDirector) throws CoreException {
        boolean isFindDuplicates = lookupDirector.isFindDuplicates();
	    try {
	        lookupDirector.setFindDuplicates(true);
	        return findKotlinSourceFile(lookupDirector, frame);
	    } finally {
	        lookupDirector.setFindDuplicates(isFindDuplicates);
	    }
    }
	
	@Nullable
	private IFile findKotlinSourceFile(@NotNull ISourceLookupDirector lookupDirector, @NotNull IJavaStackFrame frame) throws CoreException {
		String sourceName = frame.getSourceName();
		if (sourceName == null) return null;
		
		FqName declaringPackage = new FqName(frame.getDeclaringTypeName()).parent();
		
	    for (ISourceContainer sourceContainer : lookupDirector.getSourceContainers()) {
            Object[] elements = sourceContainer.findSourceElements(sourceName);
            for (Object element : elements) {
            	if (!(element instanceof IFile)) {
            		continue;
            	}
            	
            	IFile kotlinFile = (IFile) element;
            	if (fileMatches(kotlinFile, declaringPackage, sourceName)) {
            		return kotlinFile;
            	}
            }
        }
	    
	    return null;
	}
	
	private boolean fileMatches(@NotNull IFile kotlinFile, @NotNull FqName packageName, @NotNull String sourceName) {
		boolean isKotlinSourceFile = kotlinFile.getName().equals(sourceName) && KotlinPsiManager.INSTANCE.existsSourceFile(kotlinFile);
        if (isKotlinSourceFile) {
		    KtFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(kotlinFile);
		    if (jetFile.getPackageFqName().equals(packageName)) {
		        return true;
		    }
		}
		
		return false;
	}
	
}
