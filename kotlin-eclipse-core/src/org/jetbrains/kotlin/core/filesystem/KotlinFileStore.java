package org.jetbrains.kotlin.core.filesystem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.util.Util;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.core.asJava.KotlinLightClassGeneration;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.psi.JetFile;


public class KotlinFileStore extends LocalFile {
    
    public KotlinFileStore(File file) {
        super(file);
    }

    @Override
    public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
        IJavaProject javaProject = getJavaProject();
        if (javaProject == null) {
            throw new CoreException(Status.CANCEL_STATUS);
        }
        
        List<JetFile> jetFiles = KotlinLightClassManager.getInstance(javaProject).getSourceFiles(file);
        if (!jetFiles.isEmpty()) {
            AnalysisResult analysisResult = KotlinAnalysisProjectCache.INSTANCE$.getAnalysisResultIfCached(javaProject);
            if (analysisResult == null) {
                analysisResult = KotlinAnalyzer.analyzeFiles(javaProject, jetFiles).getAnalysisResult();
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
        
        throw new CoreException(Status.CANCEL_STATUS);
    }
    
    @Override
    public IFileInfo fetchInfo(int options, IProgressMonitor monitor) {
        FileInfo info = (FileInfo) super.fetchInfo(options, monitor);
        if (Util.isClassFileName(getName())) {
            IFile workspaceFile = findFileInWorkspace();
            if (workspaceFile != null) {
                info.setExists(workspaceFile.exists());
            }
        } else {
            IContainer workspaceContainer = findFolderInWorkspace();
            if (workspaceContainer != null) {
                info.setExists(workspaceContainer.exists());
                info.setDirectory(true);
            }
        }
        
        return info;
    }
    
    @Override
    public String[] childNames(int options, IProgressMonitor monitor) {
        List<String> children = new ArrayList<>();
        try {
            IContainer folder = findFolderInWorkspace();
            if (folder != null && folder.exists()) {
                for (IResource member : folder.members()) {
                    children.add(member.getName());
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return children.toArray(new String[children.size()]);
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
    private IFile findFileInWorkspace() {
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());
        if (files != null && files.length > 0) {
            assert files.length == 1 : "By " + file.toURI() + "found more than one file";
            
            return files[0];
        }
        
        return null;
    }
    
    @Nullable
    private IContainer findFolderInWorkspace() {
        IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(file.toURI());
        if (containers != null && containers.length > 0) {
            assert containers.length == 1 : "By " + file.toURI() + "found more than one file";
            return containers[0];
        }
        
        return null;
    }
    
    @Nullable
    private IJavaProject getJavaProject() {
        IFile file = findFileInWorkspace();
        return file != null ? JavaCore.create(file.getProject()) : null;
    }
}
