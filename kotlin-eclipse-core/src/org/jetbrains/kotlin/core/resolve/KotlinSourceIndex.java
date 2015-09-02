package org.jetbrains.kotlin.core.resolve;

import java.util.HashMap;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.resolve.sources.LibrarySourcesIndex;
import org.jetbrains.kotlin.idea.JetFileType;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

public class KotlinSourceIndex {
    
    private final HashMap<IPackageFragmentRoot, LibrarySourcesIndex> packageIndexes = new HashMap<>();
    
    public static KotlinSourceIndex getInstance(IJavaProject javaProject) {
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        return ServiceManager.getService(ideaProject, KotlinSourceIndex.class);
    }
    
    public static boolean isKotlinSource(String shortFileName) {
        return JetFileType.EXTENSION.equals(new Path(shortFileName).getFileExtension());
    }
    
    @Nullable
    public static char[] getSource(SourceMapper mapper, IType type, String simpleSourceFileName) {
        KotlinSourceIndex index = KotlinSourceIndex.getInstance(type.getJavaProject());
        IPackageFragment packageFragment = type.getPackageFragment();
        if (packageFragment instanceof PackageFragment) {
            String resolvedPath = index.resolvePath((PackageFragment) packageFragment, simpleSourceFileName);
            return mapper.findSource(resolvedPath);
        }
        return null;
    }
    
    public String resolvePath(PackageFragment packageFragment, String pathToSource) {
        IPackageFragmentRoot packageFragmentRoot = packageFragment.getPackageFragmentRoot();
        LibrarySourcesIndex packageIndex = getIndexForRoot(packageFragmentRoot);
        if (packageIndex == null) {
            return pathToSource;
        }
        String simpleName = new Path(pathToSource).lastSegment();
        String result = packageIndex.resolve(simpleName, packageFragment);
        return result != null ? result : pathToSource;
    }
    
    @Nullable
    private LibrarySourcesIndex getIndexForRoot(IPackageFragmentRoot packageRoot) {
        LibrarySourcesIndex result = packageIndexes.get(packageRoot);
        if (result != null) {
            return result;
        }
        try {
            if (packageRoot.getKind() != IPackageFragmentRoot.K_BINARY) {
                return null;
            }
            LibrarySourcesIndex index = new LibrarySourcesIndex(packageRoot);
            packageIndexes.put(packageRoot, index);
            return index;
        } catch (JavaModelException e) {
            KotlinLogger.logError("Unable to analyze sources for package", e);
        }
        return null;
    }
}
