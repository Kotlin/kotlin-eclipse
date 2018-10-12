package com.intellij.buildsupport.resolve.http


import groovy.transform.TupleConstructor


@TupleConstructor(includeFields = true)
abstract class HttpArtifactsResolver {

    // FIELDS =========================================================================================================
    protected final String httpBaseUrl


    // PUBLIC API =====================================================================================================
    final void downloadTo(HttpArtifact httpArtifact, File outputFile) {
        println "Downloading artifact: $httpArtifact.filePath"

        downloadFileFromUrlInto "$httpBaseUrl/$httpArtifact.filePath", outputFile
    }


    // PRIVATE API ====================================================================================================
    private void downloadFileFromUrlInto(String fileURL, File destinationFile) {
        destinationFile.parentFile.mkdirs()

        new AntBuilder().get(src:          fileURL,
                             dest:         destinationFile,
                             usetimestamp: true)
    }
}
