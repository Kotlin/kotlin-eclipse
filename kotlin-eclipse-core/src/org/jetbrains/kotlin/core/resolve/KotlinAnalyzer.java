package org.jetbrains.kotlin.core.resolve;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.NamespaceDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.kotlin.core.builder.KotlinManager;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;

import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;

public class KotlinAnalyzer {
    
    private final Project project;
    private final JavaCoreApplicationEnvironment applicationEnvironment;
    private BindingContext bindingContext;
    private static PsiElement psiTree = null;
    
    public KotlinAnalyzer() {
        KotlinEnvironment kotlinEnvironment = new KotlinEnvironment();
        applicationEnvironment = kotlinEnvironment.getApplicationEnvironment();
        project = kotlinEnvironment.getProject();
    }
    
    public static BindingContext Analyze() {
        return new KotlinAnalyzer().AnalyzeAllProjects();
    }
    
    public BindingContext AnalyzeAllProjects() {
        KotlinBuiltIns.initialize(project);
        
        BindingTraceContext context = new BindingTraceContext();
        FakeJetNamespaceDescriptor jetNamespace = new FakeJetNamespaceDescriptor();
        context.record(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, KotlinBuiltIns.getInstance().getBuiltInsPackageFqName(), jetNamespace);
        WritableScopeImpl scope = new WritableScopeImpl(JetScope.EMPTY, jetNamespace, RedeclarationHandler.THROW_EXCEPTION,
                "Builtin classes scope");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        jetNamespace.setMemberScope(scope);
        
        TopDownAnalyzer.processStandardLibraryNamespace(project, context, scope, jetNamespace, getJetFiles());
        
        bindingContext = context.getBindingContext();
        
        return bindingContext;
    }
  
    private List<JetFile> getJetFiles() {
        List<JetFile> jetFiles = new ArrayList<JetFile>();
        Set<IFile> files = new HashSet<IFile>(KotlinManager.getAllFiles());
        for (IFile file : files) {
            VirtualFile fileByPath = applicationEnvironment.getLocalFileSystem().
                    findFileByPath(file.getRawLocation().toOSString());
            jetFiles.add((JetFile) PsiManager.getInstance(project).findFile(fileByPath));
        }
        return jetFiles;
    }
    
    /*private static void scanPsiElements(ASTNode node) {
        if (node instanceof PsiElement) {
            if (psiTree == null) {
                psiTree = node.getPsi();
            } else {
                psiTree.add(node.getPsi());
            }
        }
        ASTNode[] children = node.getChildren(null);
        for (ASTNode child : children) {
            scanPsiElements(child);
        }
    }*/
    
    private static class FakeJetNamespaceDescriptor extends NamespaceDescriptorImpl {
        private WritableScope memberScope;

        private FakeJetNamespaceDescriptor() {
            super(new NamespaceDescriptorImpl(new ModuleDescriptor(Name.special("<fake_module>")),
                                              Collections.<AnnotationDescriptor>emptyList(), Name.special("<root>")),
                                              Collections.<AnnotationDescriptor>emptyList(),
                                              KotlinBuiltIns.getInstance().getBuiltInsPackage().getName());
        }

        void setMemberScope(WritableScope memberScope) {
            this.memberScope = memberScope;
        }

        @Override
        public WritableScope getMemberScope() {
            return memberScope;
        }
    }
}
