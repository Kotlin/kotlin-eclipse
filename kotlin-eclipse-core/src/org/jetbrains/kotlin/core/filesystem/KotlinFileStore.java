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
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration;
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.psi.JetFile;


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
            
            AnalysisResult analysisResult = KotlinAnalysisProjectCache.getInstance(javaProject).getCachedAnalysisResult();
            if (analysisResult == null) {
                analysisResult = KotlinAnalyzer.analyzeProject(javaProject);
            }
            
            GenerationState state = KotlinLightClassGeneration.buildLightClasses(analysisResult, javaProject, jetFiles);
            
            String requestedClassName = new Path(file.getAbsolutePath()).lastSegment();
            for (OutputFile outputFile : state.getFactory().asList()) {
                String generatedClassName = new Path(outputFile.getRelativePath()).lastSegment();
                if (requestedClassName.equals(generatedClassName)) {
                    return new ByteArrayInputStream(outputFile.asByteArray());
                }
            }
        }
        
        return super.openInputStream(options, monitor);
    }
    
    @Override
    public IFileStore mkdir(int options, IProgressMonitor monitor) throws CoreException {
        return this;
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
    
    @Override
    public IFileStore getParent() {
        File parent = file.getParentFile();
        return parent != null ? new KotlinFileStore(parent) : null;
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
