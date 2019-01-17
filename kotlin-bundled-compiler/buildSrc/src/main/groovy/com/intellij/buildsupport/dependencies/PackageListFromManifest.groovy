package com.intellij.buildsupport.dependencies

import groovy.transform.TupleConstructor

import java.util.jar.Manifest

@TupleConstructor
class PackageListFromManifest extends PackageList {
    String path

    @Override
    protected List<String> getPackageNames() {
        new Manifest(new FileInputStream(path)).mainAttributes
                .getValue("Export-Package")
                .split(',')
                *.takeWhile { it != ';' } as List<String>
    }
}
