package com.intellij.buildsupport.resolve.tc


import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

import org.jetbrains.teamcity.rest.Build
import org.jetbrains.teamcity.rest.BuildArtifact
import org.jetbrains.teamcity.rest.BuildConfigurationId
import org.jetbrains.teamcity.rest.BuildId
import org.jetbrains.teamcity.rest.BuildLocator
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory


@TupleConstructor(excludes = ['KOTLIN_COMPILER_SOURCES_JAR', 'KOTLIN_FORMATTER_JAR', 'KOTLIN_IDE_COMMON_JAR', 'KOTLIN_PLUGIN_ZIP', 'KOTLIN_TEST_DATA_ZIP'])
@CompileStatic
class TCArtifactsResolver {
    final String teamcityBaseUrl

    final boolean lastSuccessfulBuild

    final String kotlinCompilerTcBuildTypeId
    final String kotlinCompilerTcBuildId

    final String kotlinCompilerVersion
    final String kotlinIdeaCompatibleVersionMinor


    final TCArtifact KOTLIN_PLUGIN_ZIP           = new TCArtifact('',                                           "kotlin-plugin-*-IJ${kotlinIdeaCompatibleVersionMinor}*.zip")
    final TCArtifact KOTLIN_FORMATTER_JAR        = new TCArtifact('internal',                                   'kotlin-formatter.jar')
    final TCArtifact KOTLIN_IDE_COMMON_JAR       = new TCArtifact('internal',                                   'kotlin-ide-common.jar')
    final TCArtifact KOTLIN_TEST_DATA_ZIP        = new TCArtifact('internal',                                   'kotlin-test-data.zip')
    final TCArtifact KOTLIN_COMPILER_SOURCES_JAR = new TCArtifact('maven/org/jetbrains/kotlin/kotlin-compiler', 'kotlin-compiler-*-sources.jar')


    private List<TCArtifact> REQUIRED_ARTIFACTS = [KOTLIN_TEST_DATA_ZIP,
                                                   KOTLIN_PLUGIN_ZIP,
                                                   KOTLIN_IDE_COMMON_JAR,
                                                   KOTLIN_FORMATTER_JAR,
                                                   KOTLIN_COMPILER_SOURCES_JAR]


    void downloadTo(TCArtifact tcArtifact, File outputFile) {
        if (resolvedArtifactMap == null)
            resolvedArtifactMap = resolveArtifacts()

        BuildArtifact resolvedTCArtifact = resolvedArtifactMap.get(tcArtifact)

        println "Downloading artifact: $resolvedTCArtifact.fullName"

        resolvedTCArtifact.download(outputFile)
    }


    private Map<TCArtifact, BuildArtifact> resolvedArtifactMap = null

    private Map<TCArtifact, BuildArtifact> resolveArtifacts() {
        if (lastSuccessfulBuild)
            return resolveFromLastSuccessfulBuild()
        else
            return resolveFromBuildId()
    }

    private Map<TCArtifact, BuildArtifact> resolveFromBuildId() {
        Build tcBuild = TeamCityInstanceFactory.guestAuth(teamcityBaseUrl)
                                               .build(new BuildId(kotlinCompilerTcBuildId))

        println "Resolving TC buildsupport: $tcBuild"

        return resolveRequiredArtifacts(tcBuild)
    }

    private Map<TCArtifact, BuildArtifact> resolveFromLastSuccessfulBuild() {
        BuildLocator builds = TeamCityInstanceFactory.guestAuth(teamcityBaseUrl)
                                                     .builds()
                                                     .fromConfiguration(new BuildConfigurationId(kotlinCompilerTcBuildTypeId))

        if (!kotlinCompilerVersion.trim().isEmpty())
            builds.withBranch(kotlinCompilerVersion.trim())

        for (Build tcBuild in iterable(builds.all())) {
            println "Resolving TC buildsupport: $tcBuild"
            return resolveRequiredArtifacts(tcBuild)
        }
    }

    private Map<TCArtifact, BuildArtifact> resolveRequiredArtifacts(Build tcBuild) {
        Map<TCArtifact, BuildArtifact> resolvedArtifactMap = [:] as HashMap

        REQUIRED_ARTIFACTS.each { TCArtifact requiredTcArtifact ->
            BuildArtifact resolvedTcArtifact = tcBuild.findArtifact(requiredTcArtifact.fileNameRegex,
                                                                    requiredTcArtifact.fileParentPathRegex,
                                                                 true) // recursive search

            resolvedArtifactMap.put requiredTcArtifact, resolvedTcArtifact
        }

        return resolvedArtifactMap
    }

    private static <T> Iterable<T> iterable(kotlin.sequences.Sequence<T> sequence) {
        return sequence.iterator() as Iterable<T>
    }
}
