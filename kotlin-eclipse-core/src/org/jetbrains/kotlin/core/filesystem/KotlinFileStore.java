package org.jetbrains.kotlin.core.filesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.analyzer.AnalysisResult;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;


public class KotlinFileStore extends LocalFile {
    
    public KotlinFileStore(File file) {
        super(file);
    }

    @Override
    public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
        List<JetFile> jetFiles = KotlinLightClassManager.INSTANCE.getSourceFiles(file);
        if (!jetFiles.isEmpty()) {
            IJavaProject javaProject = getJavaProject();
            assert javaProject != null;
            
            AnalysisResult analysisResult = KotlinAnalyzer.analyzeDeclarations(javaProject);
            GenerationState state = KotlinLightClassGeneration.buildLightClasses(analysisResult, javaProject, jetFiles);
            
            IPath absolutePath = new Path(file.getAbsolutePath());
            for (OutputFile outputFile : state.getFactory().asList()) {
                IPath relativePath = new Path(outputFile.getRelativePath());
                if (absolutePath.toOSString().endsWith(relativePath.toOSString())) {
                    return new ByteArrayInputStream(outputFile.asByteArray());
                }
            }
        }
        
        return super.openInputStream(options, monitor);
    }
    
    @Override
    public OutputStream openOutputStream(int options, IProgressMonitor monitor) throws CoreException {
        return new ByteArrayOutputStream();
    }
    
    @Override
    public IFileStore getChild(String name) {
        return new KotlinFileStore(new File(file, name));
    }
    
    @Override
    public IFileStore getChild(IPath path) {
        return new KotlinFileStore(new File(file, path.toOSString()));
    }
    
    @Override
    public IFileStore getFileStore(IPath path) {
        return new KotlinFileStore(new Path(file.getPath()).append(path).toFile());
    }
    
    @Nullable
    private IJavaProject getJavaProject() {
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());
        if (files != null && files.length > 0) {
            return JavaCore.create(files[0].getProject());
        }
        
        return null;
    }
}
