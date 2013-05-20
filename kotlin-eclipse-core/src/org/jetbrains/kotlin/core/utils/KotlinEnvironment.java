package org.jetbrains.kotlin.core.utils;

import org.jetbrains.jet.CompilerModeProvider;
import org.jetbrains.jet.OperationModeProvider;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.plugin.JetFileType;

import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;

public class KotlinEnvironment {
    
    private final JavaCoreApplicationEnvironment applicationEnvironment;
    private final JavaCoreProjectEnvironment projectEnvironment;
    private final Project project;
    
    private final static Disposable DISPOSABLE = new Disposable() {
        
        @Override
        public void dispose() {
        }
    };
    
    public KotlinEnvironment() {
        applicationEnvironment = new JavaCoreApplicationEnvironment(DISPOSABLE);
        
        applicationEnvironment.registerFileType(JetFileType.INSTANCE, "kt");
        applicationEnvironment.registerFileType(JetFileType.INSTANCE, "jet");
        applicationEnvironment.registerParserDefinition(new JetParserDefinition());

        applicationEnvironment.getApplication().registerService(OperationModeProvider.class, new CompilerModeProvider());
        
        projectEnvironment = new JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment);
        
        project = projectEnvironment.getProject();
    }
    
    public Project getProject() {
        return project;
    }
    
    public JavaCoreProjectEnvironment getProjectEnvironment() {
        return projectEnvironment;
    }
    
    public JavaCoreApplicationEnvironment getApplicationEnvironment() {
        return applicationEnvironment;
    }
}