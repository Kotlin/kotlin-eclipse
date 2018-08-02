package org.jetbrains.kotlin.aspects.ui;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerContentProvider;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerLabelProvider;
import org.jetbrains.kotlin.ui.KotlinAwarePackageExplorerLabelProvider;

@SuppressWarnings("restriction")
public aspect PackageExplorerLabelProviderAspect {
    
    /**
     * Replaces all instances of {@link PackageExplorerLabelProvider}
     * with instances of {@link KotlinAwarePackageExplorerLabelProvider}
     * 
     * It affects classes {@link JavaNavigatorLabelProvider} and {@link PackageExplorerPart},
     * which provides icons for Project Explorer and Package Explorer, respectively. 
     */
    PackageExplorerLabelProvider around(PackageExplorerContentProvider cp) 
        : call(PackageExplorerLabelProvider.new(PackageExplorerContentProvider)) && args(cp) {
        return new KotlinAwarePackageExplorerLabelProvider(cp);
    }
}
