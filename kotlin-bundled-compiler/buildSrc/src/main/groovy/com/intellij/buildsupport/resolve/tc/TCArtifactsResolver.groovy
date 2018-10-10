package com.intellij.buildsupport.resolve.tc


import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.transform.TupleConstructor

import org.jetbrains.teamcity.rest.Build
import org.jetbrains.teamcity.rest.BuildArtifact
import org.jetbrains.teamcity.rest.BuildConfigurationId
import org.jetbrains.teamcity.rest.BuildId
import org.jetbrains.teamcity.rest.BuildLocator
import org.jetbrains.teamcity.rest.TeamCityConversationException
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory
import org.jetbrains.teamcity.rest.TeamCityQueryException


@TupleConstructor(excludes = ['KOTLIN_COMPILER_SOURCES_JAR', 'KOTLIN_FORMATTER_JAR', 'KOTLIN_IDE_COMMON_JAR', 'KOTLIN_PLUGIN_ZIP', 'KOTLIN_TEST_DATA_ZIP'])
@CompileStatic
class TCArtifactsResolver {//TODO move to child class all Kotlin compiler specific functionality
    final String teamcityBaseUrl

    final boolean lastSuccessfulBuild

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

    private Map<TCArtifact, BuildArtifact> resolvedArtifactMap = null

    // for testing purposes only
    @PackageScope
    Date untilDate = null

    void downloadTo(TCArtifact tcArtifact, File outputFile) {
        if (resolvedArtifactMap == null) {
            resolvedArtifactMap = lastSuccessfulBuild ? resolveFromLastSuccessfulBuild()
                                                      : resolveFromBuildId()
        }

        BuildArtifact resolvedTCArtifact = resolvedArtifactMap.get(tcArtifact)

        println "Downloading artifact: $resolvedTCArtifact.fullName"

        resolvedTCArtifact.download(outputFile)
    }

    private Map<TCArtifact, BuildArtifact> resolveFromBuildId() {
        Build tcBuild = TeamCityInstanceFactory.guestAuth(teamcityBaseUrl)
                                               .build(new BuildId(kotlinCompilerTcBuildId))

        println "Resolving TC build: $tcBuild"

        return resolveRequiredArtifacts(tcBuild)
    }

    private Map<TCArtifact, BuildArtifact> resolveFromLastSuccessfulBuild() {
        BuildLocator builds = TeamCityInstanceFactory.guestAuth(teamcityBaseUrl)
                                                     .builds()
                                                     .fromConfiguration(new BuildConfigurationId(tcBuildTypeId()))

        if (!kotlinCompilerVersion.trim().isEmpty())
            builds.withBranch(kotlinCompilerVersion.trim())

        if (untilDate != null)
            builds.untilDate(untilDate)

        for (Build tcBuild in iterable(builds.all())) {
            println "Resolving TC build: $tcBuild"

            Map <TCArtifact, BuildArtifact> resolvedArtifacts = resolveRequiredArtifacts(tcBuild)
            if (resolvedArtifacts.isEmpty())
                continue
            else
                return resolvedArtifacts
        }
    }

    private String tcBuildTypeId() {
        String kotlinCompilerVersionInBuildId = kotlinCompilerVersion.replace('.', '') // '1.3.0'  => '130'
                                                                     .replace('-', '') // '1.3-M2' => '13M2'

        return "Kotlin_${kotlinCompilerVersionInBuildId}_CompilerAllPlugins"
    }

    private Map<TCArtifact, BuildArtifact> resolveRequiredArtifacts(Build tcBuild) {
        Map<TCArtifact, BuildArtifact> resolvedArtifactMap = [:] as HashMap

        for (TCArtifact requiredTcArtifact in REQUIRED_ARTIFACTS) {
            try {
                BuildArtifact resolvedTcArtifact = tcBuild.findArtifact(requiredTcArtifact.fileNameRegex,
                                                                        requiredTcArtifact.fileParentPathRegex,
                                                                        true) // recursive search

                resolvedArtifactMap.put requiredTcArtifact, resolvedTcArtifact
            }
            // in the case the latest build does not contain any artifacts, we continue searching the previous build
            catch (TeamCityConversationException e) {
                return Collections.EMPTY_MAP
            }
            catch (TeamCityQueryException e) {
                return Collections.EMPTY_MAP
            }
        }

        return resolvedArtifactMap
    }

    private static <T> Iterable<T> iterable(kotlin.sequences.Sequence<T> sequence) {
        return sequence.iterator() as Iterable<T>
    }
}
