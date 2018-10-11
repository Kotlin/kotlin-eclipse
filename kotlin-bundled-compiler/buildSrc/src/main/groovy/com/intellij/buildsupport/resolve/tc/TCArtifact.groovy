package com.intellij.buildsupport.resolve.tc


import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor


@TupleConstructor(includeFields = true)
@EqualsAndHashCode
@ToString
class TCArtifact {

    final String fileParentPathRegex
    final String fileNameRegex
}
