/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
 *
 *******************************************************************************/
package org.jetbrains.kotlin.core.resolve;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.viewers.AsynchronousSchedulingRuleFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.Diagnostics;

public class AnalyzerScheduler extends Job {
    
    private final IJavaProject javaProject;
    
    public static final String FAMILY = "Analyzer";
    
    private volatile Diagnostics diagnostics = Diagnostics.EMPTY;
    
    public AnalyzerScheduler(@NotNull IJavaProject javaProject) {
        super("Analyzing " + javaProject.getElementName());
        ISchedulingRule serialRule = AsynchronousSchedulingRuleFactory.getDefault().newSerialRule();
        setRule(serialRule);
        
        this.javaProject = javaProject;
    }
    
    @Override
    public boolean belongsTo(Object family) {
        return FAMILY.equals(family);
    }
    
    public static void analyzeProjectInBackground(@NotNull IJavaProject javaProject) {
        new AnalyzerScheduler(javaProject).schedule();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        BindingContext bindingContext = KotlinAnalyzer.analyzeProject(javaProject);
        diagnostics = bindingContext.getDiagnostics();
        
        return Status.OK_STATUS;
    }
    
    @NotNull
    public Diagnostics getDiagnostics() {
        if (getState() == Job.NONE) {
            return diagnostics;
        }
        
        return Diagnostics.EMPTY;
    }
}