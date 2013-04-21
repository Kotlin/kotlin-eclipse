package org.jetbrains.kotlin.core.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;

import com.intellij.openapi.util.TextRange;

public class ProjectChangeListener implements IResourceDeltaVisitor {
    // TODO: Need to reanalize in background for every file change or on manual project rebuild.
    @Override
    public boolean visit(IResourceDelta delta) throws CoreException {
        IResource resource = delta.getResource();
        if (KotlinManager.isCompatibleResource(resource)) {
            KotlinManager.updateProjectPsiSources(resource, delta.getKind());
            
            BindingContext bindingContext = KotlinAnalyzer.analyze();
            List<Diagnostic> diagnostics = new ArrayList<Diagnostic>(bindingContext.getDiagnostics());
            for (Diagnostic diagnostic : diagnostics) {
                List<TextRange> ranges = diagnostic.getTextRanges();
                
                String position;
                if (!ranges.isEmpty()) {
                    position = ranges.get(0).toString();
                } else {
                    position = "undefined";
                }                
                
                String diagnosticMessage = String.format("%s: %s on \"%s\" at %s in %s",
                        diagnostic.getSeverity(),
                        DefaultErrorMessages.RENDERER.render(diagnostic),
                        diagnostic.getPsiElement().getText(),
                        position,
                        diagnostic.getPsiFile().getName());
                
                System.out.println(diagnosticMessage);
            }
        }
        
        return true;
    }
}