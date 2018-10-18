package com.intellij.buildsupport.resolve.http.idea


import com.intellij.buildsupport.resolve.http.HttpArtifact
import com.intellij.buildsupport.resolve.http.HttpArtifactsResolver

import groovy.transform.CompileStatic


@CompileStatic
class IntellijIdeaArtifactsResolver extends HttpArtifactsResolver {

    final String ideaVersion


    IntellijIdeaArtifactsResolver(String httpBaseUrl, String ideaVersion) {
        super(httpBaseUrl)

        this.ideaVersion = ideaVersion
    }


    final HttpArtifact INTELLIJ_CORE_ZIP   = new HttpArtifact("intellij-core/$ideaVersion/intellij-core-${ideaVersion}.zip",)
    final HttpArtifact IDEA_IC_ZIP         = new HttpArtifact("ideaIC/$ideaVersion/ideaIC-${ideaVersion}.zip",)
    final HttpArtifact IDEA_IC_SOURCES_JAR = new HttpArtifact("ideaIC/$ideaVersion/ideaIC-$ideaVersion-sources.jar",)
}
