package com.intellij.buildsupport.resolve.tc


import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import groovy.transform.TupleConstructor


@PackageScope
@TupleConstructor(includeFields = true)
@EqualsAndHashCode
class TCArtifact {

    final String fileParentPathRegex
    final String fileNameRegex
}
