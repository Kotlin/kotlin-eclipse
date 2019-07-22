package com.intellij.buildsupport.resolve.tc


import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

import org.jetbrains.teamcity.rest.Build
import org.jetbrains.teamcity.rest.BuildArtifact
import org.jetbrains.teamcity.rest.BuildConfigurationId
import org.jetbrains.teamcity.rest.BuildId
import org.jetbrains.teamcity.rest.BuildLocator
import org.jetbrains.teamcity.rest.TeamCityConversationException
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory
import org.jetbrains.teamcity.rest.TeamCityQueryException


@TupleConstructor(includeFields = true, excludes = ['resolvedArtifactMap', 'untilDate'])
@CompileStatic
abstract class TCArtifactsResolver {

    // FIELDS =========================================================================================================
    protected final String teamcityBaseUrl

    protected final boolean lastSuccessfulBuild

    protected final String tcBuildId

    protected final String tcBuildBranch


    // for testing purposes only
    protected Date untilDate = null


    private Map<TCArtifact, BuildArtifact> resolvedArtifactMap = null


    // ABSTRACT METHODS TO IMPLEMENT ==================================================================================
    abstract List<TCArtifact> getRequiredArtifacts()

    abstract String tcBuildTypeId()


    // PUBLIC API =====================================================================================================
    final void downloadTo(TCArtifact tcArtifact, File outputFile) {
        if (resolvedArtifactMap == null) {
            resolvedArtifactMap = lastSuccessfulBuild ? resolveFromLastSuccessfulBuild()
                    : resolveFromBuildId()
        }
        BuildArtifact resolvedTCArtifact = resolvedArtifactMap.get(tcArtifact)

        println "Downloading artifact: $resolvedTCArtifact.fullName"

        resolvedTCArtifact.download(outputFile)
    }

    // PRIVATE API ====================================================================================================
    private Map<TCArtifact, BuildArtifact> resolveFromBuildId() {
        Build tcBuild = TeamCityInstanceFactory.guestAuth(teamcityBaseUrl)
                .build(new BuildId(tcBuildId))

        println "Resolving TC build: $tcBuild"

        return resolveRequiredArtifacts(tcBuild)
    }

    private Map<TCArtifact, BuildArtifact> resolveFromLastSuccessfulBuild() {
        BuildLocator builds = TeamCityInstanceFactory.guestAuth(teamcityBaseUrl)
                .builds()
                .fromConfiguration(new BuildConfigurationId(tcBuildTypeId()))

        if (!tcBuildBranch.trim().isEmpty())
            builds.withBranch(tcBuildBranch.trim())

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

    private Map<TCArtifact, BuildArtifact> resolveRequiredArtifacts(Build tcBuild) {
        Map<TCArtifact, BuildArtifact> resolvedArtifactMap = [:] as HashMap

        for (TCArtifact requiredTcArtifact in getRequiredArtifacts()) {
            BuildArtifact resolvedTcArtifact

            // search the build and dependencies of the build for the artifact (in case of an aggregate build)
            for (build in [tcBuild] + tcBuild.snapshotDependencies) {
                try {
                    resolvedTcArtifact = build.findArtifact(requiredTcArtifact.fileNameRegex,
                            requiredTcArtifact.fileParentPathRegex,true)
                    break
                }
                catch (TeamCityConversationException ignored) {}
                catch (TeamCityQueryException ignored) {}
            }

            // in the case the latest build does not contain any artifacts, we continue searching the previous build
            if(resolvedTcArtifact == null)
                return Collections.EMPTY_MAP

            resolvedArtifactMap.put requiredTcArtifact, resolvedTcArtifact
        }
        return resolvedArtifactMap
    }

    private static <T> Iterable<T> iterable(kotlin.sequences.Sequence<T> sequence) {
        return sequence.iterator() as Iterable<T>
    }
}
