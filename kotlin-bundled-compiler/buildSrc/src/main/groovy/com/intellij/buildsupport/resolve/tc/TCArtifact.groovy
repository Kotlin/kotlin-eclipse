package com.intellij.buildsupport.resolve.tc


import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor


@TupleConstructor(includeFields = true)
@EqualsAndHashCode
@ToString
class TCArtifact {

    // relative to TeamCity base URL
    final String fileParentPathRegex // might contain '*' pattern
    final String fileNameRegex       // might contain '*' pattern
}
