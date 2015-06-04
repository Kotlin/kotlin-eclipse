package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ClassBuilderMode;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.core.asJava.LightClassFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.org.objectweb.asm.Type;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinLightClassManager {
    private final ConcurrentMap<File, List<File>> sourceFiles = new ConcurrentHashMap<>();
    
    @NotNull
    public static KotlinLightClassManager getInstance(@NotNull IJavaProject javaProject) {
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        return ServiceManager.getService(ideaProject, KotlinLightClassManager.class);
    }
    
    @NotNull
    public List<JetFile> getSourceFiles(@NotNull File lightClass) {
        List<File> sourceIOFiles = sourceFiles.get(lightClass);
        if (sourceIOFiles != null) {
            List<JetFile> jetSourceFiles = Lists.newArrayList();
            for (File sourceFile : sourceIOFiles) {
                JetFile jetFile = getJetFileBySourceFile(sourceFile);
                if (jetFile != null) {
                    jetSourceFiles.add(jetFile);
                }
            }
            
            return jetSourceFiles;
        }
        
        return Collections.<JetFile>emptyList();
    }
    
    
    public void updateLightClasses(
            @NotNull IJavaProject javaProject,
            @NotNull BindingContext context,
            @NotNull Set<IFile> affectedFiles) throws CoreException {
        JetTypeMapper typeMapper = new JetTypeMapper(context, ClassBuilderMode.LIGHT_CLASSES);
        IProject project = javaProject.getProject();
        Map<File, List<File>> newSourceFilesMap = new HashMap<>();
        for (IFile sourceFile : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            List<IPath> lightClassesPaths = getLightClassesPaths(sourceFile, context, typeMapper);
            
            for (IPath path : lightClassesPaths) {
                LightClassFile lightClassFile = new LightClassFile(project.getFile(path));
                createParentDirsFor(lightClassFile);
                
                if (!lightClassFile.createIfNotExists() && affectedFiles.contains(sourceFile)) {
                    lightClassFile.touchFile();
                }
                
                List<File> newSourceFiles = newSourceFilesMap.get(lightClassFile.asFile());
                if (newSourceFiles == null) {
                    newSourceFiles = new ArrayList<>();
                    newSourceFilesMap.put(lightClassFile.asFile(), newSourceFiles);
                }
                newSourceFiles.add(sourceFile.getLocation().toFile());
            }
        }
        
        sourceFiles.clear();
        sourceFiles.putAll(newSourceFilesMap);
        
        cleanOutdatedLightClasses(project);
    }

    @NotNull
    private List<IPath> getLightClassesPaths(
            @NotNull IFile sourceFile, 
            @NotNull BindingContext context,
            @NotNull JetTypeMapper typeMapper) {
        List<IPath> lightClasses = new ArrayList<IPath>();
        
        JetFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(sourceFile);
        for (JetClassOrObject classOrObject : PsiTreeUtil.findChildrenOfType(jetFile, JetClassOrObject.class)) {
            FqName fqName = classOrObject.getFqName();
            if (fqName == null) continue;
            
            ClassDescriptor descriptor = context.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, 
                    fqName.toUnsafe());
            if (descriptor != null) {
                Type asmType = typeMapper.mapClass(descriptor);
                lightClasses.add(computePathByInternalName(asmType.getInternalName()));
            }
        }
        
        if (PackagePartClassUtils.fileHasCallables(jetFile)) {
            String internalName = PackageClassUtils.getPackageClassInternalName(jetFile.getPackageFqName());
            lightClasses.add(computePathByInternalName(internalName));
        }
        
        return lightClasses;
    }
    
    private IPath computePathByInternalName(String internalName) {
        Path relativePath = new Path(internalName + ".class");
        return KotlinJavaManager.KOTLIN_BIN_FOLDER.append(relativePath);
    }
    
    private void cleanOutdatedLightClasses(IProject project) throws CoreException {
        ProjectUtils.cleanFolder(KotlinJavaManager.INSTANCE.getKotlinBinFolderFor(project), new Predicate<IResource>() {
            @Override
            public boolean apply(IResource resource) {
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    LightClassFile lightClass = new LightClassFile(file);
                    return getIOSourceFiles(lightClass.asFile()).isEmpty();
                }
                
                return false;
            }
        });
    }
    
    private void createParentDirsFor(@NotNull LightClassFile lightClassFile) {
        IFolder parent = (IFolder) lightClassFile.getResource().getParent();
        if (parent != null && !parent.exists()) {
            createParentDirs(parent);
        }
    }
    
    private void createParentDirs(IFolder folder) {
        IContainer parent = folder.getParent();
        if (!parent.exists()) {
            createParentDirs((IFolder) parent);
        }
        
        try {
            folder.create(true, true, null);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @NotNull
    private List<File> getIOSourceFiles(@NotNull File lightClass) {
        List<File> sourceIOFiles = sourceFiles.get(lightClass);
        return sourceIOFiles != null ? sourceIOFiles : Collections.<File>emptyList();
    }
    
    @Nullable
    private static IFile getEclipseFile(@NotNull File sourceFile) {
        IFile[] eclipseFile = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(sourceFile.toURI());
        return eclipseFile.length == 1 ? eclipseFile[0] : null;
    }
    
    @Nullable
    private static JetFile getJetFileBySourceFile(@NotNull File sourceFile) {
        IFile file = getEclipseFile(sourceFile);
        return file != null ? KotlinPsiManager.getKotlinParsedFile(file) : null;
    }
}