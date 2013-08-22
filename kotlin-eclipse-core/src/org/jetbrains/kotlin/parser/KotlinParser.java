package org.jetbrains.kotlin.parser;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;

import com.intellij.psi.PsiFile;

public class KotlinParser {

    private final File file;
    
    private final KotlinEnvironment kotlinEnvironment;
    
    public KotlinParser(File file, IJavaProject javaProject) {
        this.file = file;
        kotlinEnvironment = new KotlinEnvironment(javaProject);
    }
    
    public KotlinParser(@NotNull IFile iFile) {
        this(new File(iFile.getRawLocation().toOSString()), JavaCore.create(iFile.getProject()));
    }
    
    public static PsiFile getPsiFile(@NotNull IFile file) {
        return new KotlinParser(file).getPsiFile();
    }
    
    public static PsiFile getPsiFile(@NotNull File file, IJavaProject javaProject) {
        return new KotlinParser(file, javaProject).getPsiFile();
    }
    
    @Nullable
    private PsiFile getPsiFile() {
        return kotlinEnvironment.getJetFile(file);
    }
}