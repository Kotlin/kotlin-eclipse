package com.intellij.buildsupport.tc

import com.intellij.buildsupport.resolve.tc.TCArtifactsResolver
import spock.lang.*


class KotlinCompilerTCArtifactsResolverSpecification extends Specification {

    private static final String TC_BASE_URL = 'https://teamcity.jetbrains.com'


    def "should be able to resolve TC artifacts by buildId for version 1.2.72 release"() {
        setup:
            def tcArtifactResolver = tcArtifactResolverWithBuildIdAndKotlinCompilerVsersion('1646860',
            '1.2.70', // TODO remove
            '2017.3')
            def temporaryFile = File.createTempFile('kotlin-compiler-1.2.72', '.tmp')

        expect:
            tcArtifactResolver.downloadTo tcArtifactResolver.KOTLIN_PLUGIN_ZIP, temporaryFile

            assert temporaryFile.exists()
    }

    def "should be able to resolve TC artifacts by buildId for version 1.3 M2 EAP"() {
        setup:
            def tcArtifactResolver = tcArtifactResolverWithBuildIdAndKotlinCompilerVsersion('1572593',
                '1.3-M2', // TODO remove
                '2017.3')
            def temporaryFile = File.createTempFile('kotlin-compiler-1.3.EAP', '.tmp')

        expect:
            tcArtifactResolver.downloadTo tcArtifactResolver.KOTLIN_PLUGIN_ZIP, temporaryFile

            assert temporaryFile.exists()
    }

    def "should be able to resolve TC artifacts by buildId for compiler version 1.3 RC"() {
        setup:
            def tcArtifactResolver = tcArtifactResolverWithBuildIdAndKotlinCompilerVsersion('1664232',
                '1.3.0', // TODO remove
                '2017.3')
            def temporaryFile = File.createTempFile('kotlin-compiler-1.3.RC', '.tmp')

        expect:
            tcArtifactResolver.downloadTo tcArtifactResolver.KOTLIN_PLUGIN_ZIP, temporaryFile

            assert temporaryFile.exists()
    }


    private static TCArtifactsResolver tcArtifactResolverWithBuildIdAndKotlinCompilerVsersion(String tcBuildId,
                                                                                              String kotlinCompilerVersion,
                                                                                              String kotlinIdeaCompatibleVersionMinor) {
        return new TCArtifactsResolver(TC_BASE_URL,
                                      false,     // not searching for last successful build
                                      '', // TC build type ID not used
                                       tcBuildId,
                                       kotlinCompilerVersion, //TODO this should be automatically resolved from the build !!!
                                       kotlinIdeaCompatibleVersionMinor)
    }
}
