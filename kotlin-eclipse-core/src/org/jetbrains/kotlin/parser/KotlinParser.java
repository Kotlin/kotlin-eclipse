package org.jetbrains.kotlin.parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.jetbrains.jet.CompilerModeProvider;
import org.jetbrains.jet.OperationModeProvider;
import org.jetbrains.jet.lang.parsing.JetParser;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;

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

    private final static Disposable DISPOSABLE = new Disposable() {
        
        @Override
        public void dispose() {
        }
    };
    
    private final File file;
    
    private final JavaCoreApplicationEnvironment applicationEnvironment;
    private final Project project;
    private ASTNode tree;
    
    public KotlinParser(File file) {
        this.file = file;
        this.tree = null;
        
        applicationEnvironment = new JavaCoreApplicationEnvironment(DISPOSABLE);
        
        applicationEnvironment.registerFileType(JetFileType.INSTANCE, "kt");
        applicationEnvironment.registerFileType(JetFileType.INSTANCE, "jet");
        applicationEnvironment.registerParserDefinition(new JetParserDefinition());

        applicationEnvironment.getApplication().registerService(OperationModeProvider.class, new CompilerModeProvider());
        
        JavaCoreProjectEnvironment projectEnvironment = new JavaCoreProjectEnvironment(DISPOSABLE, applicationEnvironment);
        
        project = projectEnvironment.getProject();
    }
    
    public KotlinParser(IFile iFile) {
        this(new File(iFile.getRawLocation().toOSString()));
    }
    
    public static ASTNode parse(IFile iFile) {
        return new KotlinParser(iFile).parse();
    }
    
    public ASTNode parse() {
        JetParser jetParser = new JetParser(project);
        tree = jetParser.parse(null, createPsiBuilder(getNode(file)), getPsiFile(file));
        
        return tree;
    }
    
    public ASTNode getTree() {
        return  tree;
    }
    
    private PsiBuilder createPsiBuilder(ASTNode chameleon) {
        return PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, 
                JetLanguage.INSTANCE, chameleon.getChars());
    }
    
    private PsiFile getPsiFile(File file) {
        VirtualFile fileByPath = applicationEnvironment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
        
        return PsiManager.getInstance(project).findFile(fileByPath);
    }
    
    private ASTNode getNode(File file) {
        JetFile jetFile = (JetFile) getPsiFile(file);
        return jetFile.getNode();
    }
    
    public static ASTNode parseText(String document) {
        ASTNode parsedFile = null;
        
        try {
            File tempFile;
            tempFile = File.createTempFile("temp", "." + JetFileType.INSTANCE.getDefaultExtension());
            BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
            bw.write(document);
            bw.close();
            
            parsedFile = new KotlinParser(tempFile).parse();
            
            tempFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return parsedFile;
    }
}
