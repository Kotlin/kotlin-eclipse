package com.intellij.buildsupport.resolve.tc.kotlin

import com.intellij.buildsupport.resolve.tc.TCArtifact
import com.intellij.buildsupport.resolve.tc.TCArtifactsResolver
import groovy.transform.CompileStatic

@CompileStatic
class CommonIDEArtifactsResolver extends TCArtifactsResolver {

    final String kotlinCompilerVersion


    CommonIDEArtifactsResolver(
            String teamcityBaseUrl,
            boolean lastSuccessfulBuild,
            String tcBuildId,
            String kotlinCompilerVersion
    ) {
        super(teamcityBaseUrl, lastSuccessfulBuild, tcBuildId, kotlinCompilerVersion)

        this.kotlinCompilerVersion = kotlinCompilerVersion
    }

    public final TCArtifact KOTLIN_FORMATTER_JAR = new TCArtifact('internal', 'kotlin-formatter.jar')
    public final TCArtifact KOTLIN_IDE_COMMON_JAR = new TCArtifact('internal', 'kotlin-ide-common.jar')

    @Override
    List<TCArtifact> getRequiredArtifacts() {
        return [
                KOTLIN_IDE_COMMON_JAR,
                KOTLIN_FORMATTER_JAR
        ]
    }

    @Override
    String tcBuildTypeId() {
        String kotlinCompilerVersionInBuildId = kotlinCompilerVersion.replace('.', '') // '1.3.0'  => '130'
                .replace('-', '') // '1.3-M2' => '13M2'

        return "Kotlin_${kotlinCompilerVersionInBuildId}_CompilerAllPlugins"
    }
}
