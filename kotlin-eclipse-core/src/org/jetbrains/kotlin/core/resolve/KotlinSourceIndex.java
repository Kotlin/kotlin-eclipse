package org.jetbrains.kotlin.core.resolve;

import java.util.HashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.SourceMapper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.sources.LibrarySourcesIndex;
import org.jetbrains.kotlin.core.resolve.sources.LibrarySourcesIndexKt;
import org.jetbrains.kotlin.idea.KotlinFileType;

public class KotlinSourceIndex {
    
    private KotlinSourceIndex() {
    }
    
    public static final KotlinSourceIndex INSTANCE = new KotlinSourceIndex();
    
    private final HashMap<IPackageFragmentRoot, LibrarySourcesIndex> packageIndexes = new HashMap<>();
    
    public static boolean isKotlinSource(String shortFileName) {
        return KotlinFileType.EXTENSION.equals(new Path(shortFileName).getFileExtension());
    }
    
    @Nullable
    public static char[] getSource(SourceMapper mapper, IType type, String simpleSourceFileName) {
        IPackageFragment packageFragment = type.getPackageFragment();
        if (packageFragment instanceof PackageFragment) {
            String resolvedPath = INSTANCE.resolvePath((PackageFragment) packageFragment, simpleSourceFileName);
            return mapper.findSource(resolvedPath);
        }
        return null;
    }
    
    @Nullable
    public static char[] getSource(SourceMapper mapper, String sourceFileName, IPath packageFolder, IPath sourcePath) {
        LibrarySourcesIndex index = new LibrarySourcesIndex(sourcePath);
        String result = index.resolve(sourceFileName, packageFolder);
        return result != null ? 
                mapper.findSource(result) : mapper.findSource(packageFolder.append(sourceFileName).toPortableString());
    }
    
    public String resolvePath(PackageFragment packageFragment, String pathToSource) {
        IPackageFragmentRoot packageFragmentRoot = packageFragment.getPackageFragmentRoot();
        LibrarySourcesIndex packageIndex = getIndexForRoot(packageFragmentRoot);
        if (packageIndex == null) {
            return pathToSource;
        }
        String simpleName = new Path(pathToSource).lastSegment();
        String result = packageIndex.resolve(simpleName, packageFragment.getElementName());
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
            
            IPath sourcePath = LibrarySourcesIndexKt.getSourcePath(packageRoot);
            if (sourcePath == null) {
                return null;
            }
            
            LibrarySourcesIndex index = new LibrarySourcesIndex(sourcePath);
            packageIndexes.put(packageRoot, index);
            
            return index;
        } catch (JavaModelException e) {
            KotlinLogger.logError("Unable to analyze sources for package", e);
        }
        return null;
    }
}
