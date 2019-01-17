package com.intellij.buildsupport.dependencies

import groovy.transform.TupleConstructor

import java.util.jar.Manifest

@TupleConstructor
class PackageListFromSimpleFile extends PackageList {
    String path

    @Override
    protected List<String> getPackageNames() {
        new FileInputStream(path).readLines()
                *.trim()
                .findAll { !it.empty }
                .findAll { it.take(1) != '#' }
    }
}
