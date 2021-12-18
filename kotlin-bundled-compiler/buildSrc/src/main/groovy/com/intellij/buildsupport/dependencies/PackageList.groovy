package com.intellij.buildsupport.dependencies

abstract class PackageList {
    List<String> getPathsToInclude() {
        List<String> tempList = []
        packageNames.forEach {
            if(it.startsWith("custom:")) {
                tempList.add(it.replace("custom:", ""))
            } else {
                tempList.add(it.replace('.', '/') + '/*.class')
            }
        }
        return tempList
    }

    protected abstract List<String> getPackageNames()
}