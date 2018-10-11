package com.intellij.buildsupport.resolve.http


import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.transform.TupleConstructor


@TupleConstructor(includeFields = true)
@EqualsAndHashCode
@ToString
class HttpArtifact {

    // relative to HTTP base URL
    final String filePath // cannot contain any regex patterns
}
