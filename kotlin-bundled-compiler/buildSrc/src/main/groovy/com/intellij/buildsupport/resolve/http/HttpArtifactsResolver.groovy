package com.intellij.buildsupport.resolve.http


import groovy.transform.TupleConstructor


@TupleConstructor(includeFields = true)
class HttpArtifactsResolver {

    // FIELDS =========================================================================================================
    protected final String httpBaseUrl

    static Map<String, Object> proxyProps = new HashMap<>()


    // PUBLIC API =====================================================================================================
    final void downloadTo(HttpArtifact httpArtifact, File outputFile) {
        println "Downloading artifact: $httpArtifact.filePath"

        downloadFileFromUrlInto "$httpBaseUrl/$httpArtifact.filePath", outputFile
    }


    // PRIVATE API ====================================================================================================
    private void downloadFileFromUrlInto(String fileURL, File destinationFile) {
        destinationFile.parentFile.mkdirs()

        def ant = new AntBuilder()
        if (!proxyProps.isEmpty()) {
            if (proxyProps.get("https.proxyUser") == null) {
                ant.setproxy(proxyHost: proxyProps['https.proxyHost'], proxyPort: proxyProps['https.proxyPort'])
            } else {
                ant.setproxy(proxyHost: proxyProps['https.proxyHost'], proxyPort: proxyProps['https.proxyPort'], proxyUser: proxyProps['https.proxyUser'], proxyPassword: proxyProps['https.proxyPassword'])
            }
        }
        ant.get(src: fileURL,
                dest: destinationFile,
                usetimestamp: true)
    }
}
