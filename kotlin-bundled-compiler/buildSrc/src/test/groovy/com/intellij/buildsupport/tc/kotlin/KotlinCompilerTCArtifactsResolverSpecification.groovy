package com.intellij.buildsupport.tc.kotlin


import com.intellij.buildsupport.resolve.tc.TCArtifactsResolver
import com.intellij.buildsupport.resolve.tc.kotlin.KotlinCompilerTCArtifactsResolver

import java.text.SimpleDateFormat

import org.junit.Rule
import org.junit.contrib.java.lang.system.SystemOutRule

import spock.lang.Specification
import spock.lang.Unroll

import static org.apache.commons.lang3.StringUtils.countMatches


class KotlinCompilerTCArtifactsResolverSpecification extends Specification {

    private static final String TC_BASE_URL = 'https://teamcity.jetbrains.com'

    @Rule
    public final SystemOutRule systemOut = new SystemOutRule().enableLog()


    @Unroll
    def "should resolve TC artifacts by buildId for version #releaseDescribeVersion"() {
        setup:
            def tcArtifactResolver = tcArtifactResolverWithBuildId(tcBuildId,
                                                                   kotlinIdeaCompatibleVersionMinor)

            def temporaryFile = File.createTempFile("kotlin-compiler-$releaseDescribeVersion", '.tmp')

        expect:
            tcArtifactResolver.downloadTo tcArtifactResolver.KOTLIN_PLUGIN_ZIP, temporaryFile

            assert temporaryFile.exists()

        where:
            releaseDescribeVersion | tcBuildId | kotlinIdeaCompatibleVersionMinor
            '1.2.72-release-68'    | '1646860' | '2017.3'
            '1.3-M2-eap-105'       | '1572593' | '2017.3'
            '1.3.0-rc-153'         | '1664232' | '2017.3'
    }

    @Unroll
    def "should resolve TC artifacts for latest successful build for version #kotlinCompilerVersion"() {
        setup:
            def tcArtifactResolver = tcArtifactResolverWithLatestBuildAndKotlinCompilerVersion(kotlinCompilerVersion,
                                                                                               kotlinIdeaCompatibleVersionMinor)

            def temporaryFile = File.createTempFile("kotlin-compiler-latest-$kotlinCompilerVersion", '.tmp')

        expect:
            tcArtifactResolver.downloadTo tcArtifactResolver.KOTLIN_PLUGIN_ZIP, temporaryFile

            assert temporaryFile.exists()

        where:
            kotlinCompilerVersion | kotlinIdeaCompatibleVersionMinor
            '1.2.70'              | '2017.3'
            '1.3-M1'              | '2017.3'
            '1.3-M2'              | '2017.3'
            '1.3.0'               | '2017.3'
    }

    @Unroll
    def "should resolve TC artifacts for latest successful build without TC artifacts for version #kotlinCompilerVersion"() {
        setup:
            def tcArtifactResolver = tcArtifactResolverWithLatestBuildAndKotlinCompilerVersion(kotlinCompilerVersion,
                                                                                               kotlinIdeaCompatibleVersionMinor,
                                                                                               untilDate)

            def temporaryFile = File.createTempFile("kotlin-compiler-latest-$kotlinCompilerVersion", '.tmp')

        expect:
            tcArtifactResolver.downloadTo tcArtifactResolver.KOTLIN_PLUGIN_ZIP, temporaryFile

            assert temporaryFile.exists()

            assert countMatches(systemOut.log, 'Resolving TC build') == expectednumberOfSearchedTcBuilds

        where:
            untilDate    | kotlinCompilerVersion | kotlinIdeaCompatibleVersionMinor || expectednumberOfSearchedTcBuilds
            '2018-08-16' | '1.2.60'              | '2017.3'                         || 4
            '2018-09-10' | '1.2.70'              | '2017.3'                         || 5
            '2018-08-24' | '1.3-M2'              | '2017.3'                         || 4
            '2018-10-03' | '1.3.0'               | '2017.3'                         || 11
    }


    private static TCArtifactsResolver tcArtifactResolverWithBuildId(String tcBuildId,
                                                                     String kotlinIdeaCompatibleVersionMinor) {
        return new KotlinCompilerTCArtifactsResolver(TC_BASE_URL,
                                                     false,     // not searching for last successful build
                                                     tcBuildId,
                                                     '', // Kotlin compiler version determined automatically
                                                     kotlinIdeaCompatibleVersionMinor)
    }

    private static TCArtifactsResolver tcArtifactResolverWithLatestBuildAndKotlinCompilerVersion(String kotlinCompilerVersion,
                                                                                                 String kotlinIdeaCompatibleVersionMinor) {
        return tcArtifactResolverWithLatestBuildAndKotlinCompilerVersion(kotlinCompilerVersion,
                                                                         kotlinIdeaCompatibleVersionMinor,
                                                                         null)
    }

    private static TCArtifactsResolver tcArtifactResolverWithLatestBuildAndKotlinCompilerVersion(String kotlinCompilerVersion,
                                                                                                 String kotlinIdeaCompatibleVersionMinor,
                                                                                                 String untilDate) {
        TCArtifactsResolver tcArtifactsResolver =  new KotlinCompilerTCArtifactsResolver(TC_BASE_URL,
                                                                                         true,  // searching for last successful build
                                                                                         '', // buildId not used
                                                                                         kotlinCompilerVersion,
                                                                                         kotlinIdeaCompatibleVersionMinor)

        if (untilDate != null)
            tcArtifactsResolver.untilDate = new SimpleDateFormat("yyyy-MM-dd").parse(untilDate)

        return tcArtifactsResolver
    }
}
