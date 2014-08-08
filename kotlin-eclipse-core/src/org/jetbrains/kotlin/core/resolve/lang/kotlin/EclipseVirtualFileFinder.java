package org.jetbrains.kotlin.core.resolve.lang.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.jvm.compiler.ClassPath;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinBinaryClassCache;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClassFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.vfs.VirtualFile;

public class EclipseVirtualFileFinder extends VirtualFileKotlinClassFinder implements VirtualFileFinder {
    @NotNull
    private final ClassPath classPath;

    public EclipseVirtualFileFinder(@NotNull ClassPath path) {
        classPath = path;
    }

    @Nullable
    @Override
    public VirtualFile findVirtualFileWithHeader(@NotNull FqName className) {
        for (VirtualFile root : classPath) {
            VirtualFile fileInRoot = findFileInRoot(className.asString(), root, '.');
            //NOTE: currently we use VirtualFileFinder to find Kotlin binaries only
            if (fileInRoot != null && KotlinBinaryClassCache.getKotlinBinaryClass(fileInRoot) != null) {
                return fileInRoot;
            }
        }
        return null;
    }
    
    @NotNull
    private static String classFileName(@NotNull JavaClass jClass) {
        JavaClass outerClass = jClass.getOuterClass();
        if (outerClass == null) return jClass.getName().asString();
        return classFileName(outerClass) + "$" + jClass.getName().asString();
    }
    
    @Override
    @Nullable
    public KotlinJvmBinaryClass findKotlinClass(@NotNull JavaClass javaClass) {
        FqName fqName = javaClass.getFqName();
        if (fqName == null) {
            return null;
        }
        VirtualFile file = findVirtualFileWithHeader(fqName);
        if (file == null) {
            return null;
        }
        if (javaClass.getOuterClass() != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.getParent().findChild(classFileName(javaClass) + ".class");
            assert file != null : "Virtual file not found for " + javaClass;
        }

        if (file.getFileType() != JavaClassFileType.INSTANCE) return null;

        return KotlinBinaryClassCache.getKotlinBinaryClass(file);
    }

    @Override
    public VirtualFile findVirtualFile(@NotNull String internalName) {
        for (VirtualFile root : classPath) {
            VirtualFile fileInRoot = findFileInRoot(internalName, root, '/');
            if (fileInRoot != null) {
                return fileInRoot;
            }
        }
        return null;
    }

    //NOTE: copied with some changes from CoreJavaFileManager
    @Nullable
    private static VirtualFile findFileInRoot(@NotNull String qName, @NotNull VirtualFile root, char separator) {
        String pathRest = qName;
        VirtualFile cur = root;

        while (true) {
            int dot = pathRest.indexOf(separator);
            if (dot < 0) break;

            String pathComponent = pathRest.substring(0, dot);
            VirtualFile child = cur.findChild(pathComponent);

            if (child == null) break;
            pathRest = pathRest.substring(dot + 1);
            cur = child;
        }

        String className = pathRest.replace('.', '$');
        VirtualFile vFile = cur.findChild(className + ".class");
        if (vFile != null) {
            if (!vFile.isValid()) {
                //TODO: log
                return null;
            }
            return vFile;
        }
        return null;
    }

}
