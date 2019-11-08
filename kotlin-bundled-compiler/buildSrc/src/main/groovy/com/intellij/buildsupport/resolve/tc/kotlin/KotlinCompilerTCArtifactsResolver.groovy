package com.intellij.buildsupport.resolve.tc.kotlin


import com.intellij.buildsupport.resolve.tc.TCArtifact
import com.intellij.buildsupport.resolve.tc.TCArtifactsResolver

import groovy.transform.CompileStatic


@CompileStatic
class KotlinCompilerTCArtifactsResolver extends TCArtifactsResolver {

    final String kotlinIdeaCompatibleVersionMinor

    final String kotlinCompilerVersion


    KotlinCompilerTCArtifactsResolver(String teamcityBaseUrl, boolean lastSuccessfulBuild, String tcBuildId,
                                      String kotlinCompilerVersion, String kotlinIdeaCompatibleVersionMinor) {
        super(teamcityBaseUrl,
              lastSuccessfulBuild,
              tcBuildId,
              kotlinCompilerVersion)

        this.kotlinCompilerVersion            = kotlinCompilerVersion
        this.kotlinIdeaCompatibleVersionMinor = kotlinIdeaCompatibleVersionMinor
    }


    public final TCArtifact KOTLIN_PLUGIN_ZIP           = new TCArtifact('',                                           "kotlin-plugin-*-IJ${kotlinIdeaCompatibleVersionMinor}*.zip")
    public final TCArtifact KOTLIN_TEST_DATA_ZIP        = new TCArtifact('internal',                                   'kotlin-test-data.zip')
    public final TCArtifact KOTLIN_COMPILER_SOURCES_JAR = new TCArtifact('maven/org/jetbrains/kotlin/kotlin-compiler', 'kotlin-compiler-*-sources.jar')


    @Override
    List<TCArtifact> getRequiredArtifacts() {
        return [KOTLIN_TEST_DATA_ZIP,
                KOTLIN_PLUGIN_ZIP,
                KOTLIN_COMPILER_SOURCES_JAR]
    }

    @Override
    String tcBuildTypeId() {
        String kotlinCompilerVersionInBuildId = kotlinCompilerVersion.replace('.', '') // '1.3.0'  => '130'
                                                                     .replace('-', '') // '1.3-M2' => '13M2'

        return "Kotlin_${kotlinCompilerVersionInBuildId}_Aggregate"
    }
}
