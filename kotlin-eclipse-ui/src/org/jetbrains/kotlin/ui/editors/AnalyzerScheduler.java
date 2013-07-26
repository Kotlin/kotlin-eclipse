package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.viewers.AsynchronousSchedulingRuleFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;

public class AnalyzerScheduler extends Job {
    
    private final IJavaProject javaProject;
    
    public static final String FAMILY = "Analyzer";
    
    private volatile List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
    
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
        diagnostics = new ArrayList<Diagnostic>(bindingContext.getDiagnostics());
        
        return Status.OK_STATUS;
    }
    
    @Nullable
    public List<Diagnostic> getDiagnostics() {
        if (getState() == Job.NONE) {
            return diagnostics;
        }
        
        return null;
    }
}