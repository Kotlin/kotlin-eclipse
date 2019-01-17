package com.intellij.buildsupport.dependencies

abstract class PackageList {
    List<String> getPathsToInclude() {
        packageNames.collect { it.replace('.', '/') + '/*.class'}
    }

    protected abstract List<String> getPackageNames()
}