package org.jetbrains.kotlin.core.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;

public class ProjectChangeListener implements IResourceDeltaVisitor {

    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        if (KotlinManager.isCompatibleResource(resource)) {
            KotlinManager.updateProjectPsiSources(resource, delta.getKind());
            
            BindingContext bindingContext = KotlinAnalyzer.Analyze();
            List<Diagnostic> diagnostics = new ArrayList<Diagnostic>(bindingContext.getDiagnostics());
            for (Diagnostic diagnostic : diagnostics) {
                System.out.println("Error: " + (diagnostic.getSeverity() == Severity.ERROR) + 
                        " \"" + diagnostic.getPsiElement().getText() + "\" in " + diagnostic.getPsiFile().getName());
            }
        }
        
        return true;
    }
}