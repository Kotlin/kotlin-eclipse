package org.jetbrains.kotlin.core.utils;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

import com.google.common.collect.Lists;

public class KotlinSearchPackageRequestor extends SearchRequestor {

    private final List<IPackageFragment> collector = Lists.newArrayList();
    
    @Override
    public void acceptSearchMatch(SearchMatch match) throws CoreException {
        if (match.getElement() instanceof IPackageFragment) {
            collector.add((IPackageFragment) match.getElement());
        }
    }
    
    @Override
    public void beginReporting() {
        collector.clear();
    }
    
    public List<IPackageFragment> getPackages() {
        return Collections.unmodifiableList(collector);
    }
}
