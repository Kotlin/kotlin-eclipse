package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.internal.jobs.JobStatus;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.internal.core.util.LRUCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.asJava.LightClassFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.fileClasses.FileClasses;
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtSecondaryConstructor;
import org.jetbrains.kotlin.psi.KtVisitorVoid;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

import kotlin.jvm.functions.Function1;

public class KotlinLightClassManager {
    private static final int LIGHT_CLASSES_CACHE_SIZE = 300;

    private static final String WORKSPACE_JOB_ID = "updateLightClassesJob";
    
    private final LRUCache cachedLightClasses = new LRUCache(LIGHT_CLASSES_CACHE_SIZE);
    
    private final IProject project;
    
    private final ConcurrentMap<File, Set<IFile>> sourceFiles = new ConcurrentHashMap<>();
    
    @NotNull
    public static KotlinLightClassManager getInstance(@NotNull IProject project) {
        Project ideaProject = KotlinEnvironment.Companion.getEnvironment(project).getProject();
        return ServiceManager.getService(ideaProject, KotlinLightClassManager.class);
    }
    
    public KotlinLightClassManager(@NotNull IProject project) {
        this.project = project;
    }
    
    @Nullable
    public synchronized byte[] getCachedLightClass(File file) {
        Object lightClass = cachedLightClasses.get(file);
        if (lightClass != null) return (byte[]) lightClass;
        
        return null;
    }
    
    public synchronized void cacheLightClass(File file, @NotNull byte[] lightClass) {
        cachedLightClasses.put(file, lightClass);
    }
    
    public synchronized void removeLightClass(@NotNull File file) {
        cachedLightClasses.flush(file);
    }
    
    public void computeLightClassesSources() {
        Map<File, Set<IFile>> newSourceFilesMap = new HashMap<>();
        for (IFile sourceFile : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            List<IPath> lightClassesPaths = getLightClassesPaths(sourceFile);
            
            for (IPath path : lightClassesPaths) {
                LightClassFile lightClassFile = new LightClassFile(project.getFile(path));
                
                Set<IFile> newSourceFiles = newSourceFilesMap.get(lightClassFile.asFile());
                if (newSourceFiles == null) {
                    newSourceFiles = new HashSet<>();
                    newSourceFilesMap.put(lightClassFile.asFile(), newSourceFiles);
                }
                newSourceFiles.add(sourceFile);
            }
        }
        
        sourceFiles.clear();
        sourceFiles.putAll(newSourceFilesMap);
    }

    public void updateLightClasses(@NotNull Set<IFile> affectedFiles, Boolean resourceTreeBlocked) {
        List<LightClassFile> toCreate = new ArrayList<>();
        List<LightClassFile> toRemove = new ArrayList<>();
        for (Map.Entry<File, Set<IFile>> entry : sourceFiles.entrySet()) {
            IFile lightClassIFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(entry.getKey().getPath()));
            if (lightClassIFile == null) continue;
            
            LightClassFile lightClassFile = new LightClassFile(lightClassIFile);
            
            createParentDirsFor(lightClassFile);
            if (!lightClassFile.exists()) {
                toCreate.add(lightClassFile);
            }
            
            for (IFile sourceFile : entry.getValue()) {
                if (affectedFiles.contains(sourceFile)) {
                    toRemove.add(lightClassFile);
                    break;
                }
            }
        }
        if (resourceTreeBlocked) {
            if (!toCreate.isEmpty() || !toRemove.isEmpty()) {
                WorkspaceJob job = new WorkspaceJob(WORKSPACE_JOB_ID) {
                    @Override
                    public IStatus runInWorkspace(IProgressMonitor monitor) {
                        monitor.beginTask("Light class generation started", 0);
                        updateLightClasses(toCreate, toRemove);
                        monitor.done();
                        return new JobStatus(0, this, "Light classes generation finished");
                    }
                };
                job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().createRule(
                        project.getFolder(KotlinJavaManager.KOTLIN_BIN_FOLDER)));
                job.schedule();
            }
        } else {
            updateLightClasses(toCreate, toRemove);
        }
    }

    private void updateLightClasses(List<LightClassFile> toCreate, List<LightClassFile> toRemove) {
        for (LightClassFile lightClassFile: toCreate) {
            lightClassFile.createIfNotExists();
        }
        for (LightClassFile lightClassFile: toRemove) {
            removeLightClass(lightClassFile.asFile());
            lightClassFile.touchFile();
        }
        cleanOutdatedLightClasses(project);
    }
    
    public List<KtFile> getSourceFiles(@NotNull File file) {
        if (sourceFiles.isEmpty()) {
            computeLightClassesSources();
        }
        
        return getSourceKtFiles(file);
    }
    
    @Nullable
    public static String getInternalName(KtClassOrObject classOrObject) {
        FqName fullFqName = classOrObject.getFqName();
        if (fullFqName == null) return null;
        
        KtClassOrObject topmostClassOrObject = PsiTreeUtil.getTopmostParentOfType(classOrObject, KtClassOrObject.class);
        if (topmostClassOrObject == null) return makeInternalByToplevel(fullFqName);
        
        FqName topLevelFqName = topmostClassOrObject.getFqName();
        if (topLevelFqName == null) return null;
        
        String nestedPart = fullFqName.asString().substring(topLevelFqName.asString().length()).replaceAll("\\.", "\\$");
        
        return makeInternalByToplevel(topLevelFqName) + nestedPart;
    }
    
    private static String makeInternalByToplevel(FqName fqName) {
        return fqName.asString().replaceAll("\\.", "/");
    }
    
    @NotNull
    private List<KtFile> getSourceKtFiles(@NotNull File lightClass) {
        Set<IFile> sourceIOFiles = sourceFiles.get(lightClass);
        if (sourceIOFiles != null) {
            List<KtFile> jetSourceFiles = new ArrayList<>();
            for (IFile sourceFile : sourceIOFiles) {
                KtFile jetFile = KotlinPsiManager.getKotlinParsedFile(sourceFile);
                if (jetFile != null) {
                    jetSourceFiles.add(jetFile);
                }
            }
            
            return jetSourceFiles;
        }
        
        return Collections.<KtFile>emptyList();
    }

    @NotNull
    private List<IPath> getLightClassesPaths(@NotNull IFile sourceFile) {
        List<IPath> lightClasses = new ArrayList<IPath>();
        
        KtFile ktFile = KotlinPsiManager.INSTANCE.getParsedFile(sourceFile);
        for (KtClassOrObject classOrObject : findLightClasses(ktFile)) {
            String internalName = getInternalName(classOrObject);
            if (internalName != null) {
                lightClasses.add(computePathByInternalName(internalName));
            }
        }
        
        if (ktFile.hasTopLevelCallables()) {
            String newFacadeInternalName = FileClasses.getFileClassInternalName(
                    NoResolveFileClassesProvider.INSTANCE, ktFile);
            lightClasses.add(computePathByInternalName(newFacadeInternalName));
        }
        
        return lightClasses;
    }
    
    private List<KtClassOrObject> findLightClasses(@NotNull KtFile ktFile) {
        final ArrayList<KtClassOrObject> lightClasses = new ArrayList<KtClassOrObject>();
        ktFile.acceptChildren(new KtVisitorVoid() {
            @Override
            public void visitClassOrObject(@NotNull KtClassOrObject classOrObject) {
                lightClasses.add(classOrObject);
                super.visitClassOrObject(classOrObject);
            }
            
            @Override
            public void visitNamedFunction(@NotNull KtNamedFunction function) {
            }
            
            @Override
            public void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor) {
            }

            @Override
            public void visitProperty(@NotNull KtProperty property) {
            }

            @Override
            public void visitElement(@Nullable PsiElement element) {
                if (element != null) {
                    element.acceptChildren(this);
                }
            }
        });
        return lightClasses;
    }
    
    private IPath computePathByInternalName(String internalName) {
        Path relativePath = new Path(internalName + ".class");
        return KotlinJavaManager.KOTLIN_BIN_FOLDER.append(relativePath);
    }
    
    private void cleanOutdatedLightClasses(IProject project) {
        ProjectUtils.cleanFolder(KotlinJavaManager.INSTANCE.getKotlinBinFolderFor(project), resource -> {
            if (resource instanceof IFile) {
                IFile eclipseFile = (IFile) resource;
                LightClassFile lightClass = new LightClassFile(eclipseFile);
                Set<IFile> sources = sourceFiles.get(lightClass.asFile());

                boolean dropLightClass = sources == null || sources.isEmpty();
                if (dropLightClass) {
                    removeLightClass(lightClass.asFile());
                }

                return dropLightClass;
            } else if (resource instanceof IFolder) {
                try {
                    return ((IFolder) resource).members().length == 0;
                } catch (CoreException e) {
                    KotlinLogger.logAndThrow(e);
                }
            }

            return false;
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
}