package Kotlin_KotlinEclipse.buildTypes

import jetbrains.buildServer.configs.kotlin.v2017_2.*
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.v2017_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2017_2.triggers.vcs

object Kotlin_KotlinEclipse_EclipsePluginDev : BuildType({
    uuid = "2179f557-b5d5-4a1f-b98d-87264e9ce916"
    id = "Kotlin_KotlinEclipse_EclipsePluginDev"
    name = "Eclipse Plugin Dev"

    vcs {
        root(Kotlin_KotlinEclipse.vcsRoots.Kotlin_KotlinEclipse_HttpsGithubComJetBrainsKotlinEclipseRefsHeadsMaster)
        root(Kotlin_KotlinEclipse.vcsRoots.Kotlin_KotlinEclipse_HttpsGithubComWpopielarskiKotlinEclipse)

        checkoutMode = CheckoutMode.ON_AGENT
    }

    steps {
        ant {
            mode = antFile {
                path = "kotlin-bundled-compiler/get_bundled.xml"
            }
            workingDir = "kotlin-bundled-compiler"
            targets = "get_bundled"
        }
        maven {
            goals = "clean install"
            mavenVersion = custom {
                path = "%teamcity.tool.maven.3.5.2%"
            }
            jdkHome = "%env.JDK_18_x64%"
            runnerArgs = "-e"
            param("org.jfrog.artifactory.selectedDeployableServer.defaultModuleVersionConfiguration", "GLOBAL")
        }
    }

    triggers {
        vcs {
        }
    }
})
