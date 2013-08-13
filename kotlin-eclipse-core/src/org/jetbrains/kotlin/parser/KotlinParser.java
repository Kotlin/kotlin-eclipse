package org.jetbrains.kotlin.parser;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.parsing.JetParser;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;

import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.core.JavaCoreProjectEnvironment;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

public class KotlinParser {

    private final File file;
    
    private ASTNode tree;
    private final JavaCoreApplicationEnvironment applicationEnvironment;
    private final Project project;
    
    private static final Disposable DISPOSABLE = new Disposable() {
        
        @Override
        public void dispose() {
        }
    };
    
    public KotlinParser(File file) {
        this.file = file;
        this.tree = null;
        
        applicationEnvironment = KotlinEnvironment.getApplicationEnvironment();
        JavaCoreProjectEnvironment projectEnvironment = new JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment);
        
        project = projectEnvironment.getProject();
    }
    
    public KotlinParser(@NotNull IFile iFile) {
        this(new File(iFile.getRawLocation().toOSString()));
    }
    
    @NotNull
    public static ASTNode parse(@NotNull IFile iFile) {
        return new KotlinParser(iFile).parse();
    }
    
    public static PsiFile getPsiFile(@NotNull IFile file) {
        return new KotlinParser(file).getPsiFile();
    }
    
    @NotNull
    public ASTNode parse() {
        JetParser jetParser = new JetParser(project);
        tree = jetParser.parse(null, createPsiBuilder(getNode()), getPsiFile());
        
        return tree;
    }
    
    @NotNull
    private PsiBuilder createPsiBuilder(ASTNode chameleon) {
        return PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, 
                JetLanguage.INSTANCE, chameleon.getChars());
    }
    
    @Nullable
    private PsiFile getPsiFile() {
        String path = file.getAbsolutePath();
        
        if (path == null) {
            return null;
        }
        
        VirtualFile fileByPath = applicationEnvironment.getLocalFileSystem().findFileByPath(path);
        
        if (fileByPath == null) {
            return null;
        }
        
        return PsiManager.getInstance(project).findFile(fileByPath);
    }
    
    @Nullable
    private ASTNode getNode() {
        JetFile jetFile = (JetFile) getPsiFile();
        if (jetFile != null) {
            return jetFile.getNode();
        }
        
        return null;
    }
}
