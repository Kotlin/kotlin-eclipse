package org.jetbrains.kotlin.core.model;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetFileType;

public class KotlinModelUtils {
    public static void excludeKotlinFilesFromOutput(@NotNull IJavaProject javaProject) {
        String excludeFiles = javaProject.getOption(JavaCore.CORE_JAVA_BUILD_RESOURCE_COPY_FILTER, true);
        String ktFilesPattern = "*." + JetFileType.INSTANCE.getDefaultExtension();
        if (excludeFiles.contains(ktFilesPattern)) {
            if (!excludeFiles.isEmpty()) {
                excludeFiles.concat(",");
            }
            excludeFiles.concat(ktFilesPattern);
        }
    }
}
