package com.intellij.buildsupport.tc


import com.intellij.buildsupport.resolve.tc.TCArtifactsResolver
import spock.lang.Specification
import spock.lang.Unroll


class KotlinCompilerTCArtifactsResolverSpecification extends Specification {

    private static final String TC_BASE_URL = 'https://teamcity.jetbrains.com'


    @Unroll
    def "should be able to resolve TC artifacts by buildId for version #releaseDescribeVersion"() {
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
    def "should be able to resolve TC artifacts by latest build for version #releaseDescribeVersion"() {
        setup:
            def tcArtifactResolver = tcArtifactResolverWithLatestBuildAndKotlinCompilerVersion(kotlinCompilerVersion,
                                                                                               kotlinIdeaCompatibleVersionMinor)
            def temporaryFile = File.createTempFile("kotlin-compiler-latest-$kotlinCompilerVersion", '.tmp')

        expect:
            tcArtifactResolver.downloadTo tcArtifactResolver.KOTLIN_PLUGIN_ZIP, temporaryFile

            assert temporaryFile.exists()

        where:
            releaseDescribeVersion | kotlinCompilerVersion | kotlinIdeaCompatibleVersionMinor
            '1.2.70'               | '1.2.70'              | '2017.3'
            '1.3-M1'               | '1.3-M1'              | '2017.3'
            '1.3-M2'               | '1.3-M2'              | '2017.3'
            '1.3.0'                | '1.3.0'               | '2017.3'
    }


    private static TCArtifactsResolver tcArtifactResolverWithBuildId(String tcBuildId,
                                                                     String kotlinIdeaCompatibleVersionMinor) {
        return new TCArtifactsResolver(TC_BASE_URL,
                                      false,     // not searching for last successful build
                                       tcBuildId,
                                       '', // Kotlin compiler version determined automatically
                                       kotlinIdeaCompatibleVersionMinor)
    }

    private static TCArtifactsResolver tcArtifactResolverWithLatestBuildAndKotlinCompilerVersion(String kotlinCompilerVersion,
                                                                                                 String kotlinIdeaCompatibleVersionMinor) {
        return new TCArtifactsResolver(TC_BASE_URL,
                true,  // searching for last successful build
                '', // buildId not used
                kotlinCompilerVersion,
                kotlinIdeaCompatibleVersionMinor)
    }
}
