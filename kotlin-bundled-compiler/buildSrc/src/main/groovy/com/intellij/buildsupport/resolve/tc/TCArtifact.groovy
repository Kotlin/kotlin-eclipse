package com.intellij.buildsupport.resolve.tc


import groovy.transform.EqualsAndHashCode
import groovy.transform.PackageScope
import groovy.transform.ToString
import groovy.transform.TupleConstructor


@PackageScope
@TupleConstructor(includeFields = true)
@EqualsAndHashCode
@ToString
class TCArtifact {

    final String fileParentPathRegex
    final String fileNameRegex
}
