package Kotlin_KotlinEclipse

import Kotlin_KotlinEclipse.buildTypes.*
import Kotlin_KotlinEclipse.vcsRoots.*
import Kotlin_KotlinEclipse.vcsRoots.Kotlin_KotlinEclipse_HttpsGithubComWpopielarskiKotlinEclipse
import jetbrains.buildServer.configs.kotlin.v2017_2.*
import jetbrains.buildServer.configs.kotlin.v2017_2.Project
import jetbrains.buildServer.configs.kotlin.v2017_2.projectFeatures.VersionedSettings
import jetbrains.buildServer.configs.kotlin.v2017_2.projectFeatures.versionedSettings

object Project : Project({
    uuid = "50370339-ba28-41a1-8570-e9a59ac7d52f"
    id = "Kotlin_KotlinEclipse"
    parentId = "Kotlin"
    name = "Kotlin Eclipse"

    vcsRoot(Kotlin_KotlinEclipse_HttpsGithubComJetBrainsKotlinEclipseRefsHeadsMaster)
    vcsRoot(Kotlin_KotlinEclipse_HttpsGithubComWpopielarskiKotlinEclipseRefsHeadsBumpTo1230)
    vcsRoot(Kotlin_KotlinEclipse_HttpsGithubComWpopielarskiKotlinEclipse)

    buildType(Kotlin_KotlinEclipse_EclipsePluginDev)

    features {
        versionedSettings {
            id = "PROJECT_EXT_192"
            mode = VersionedSettings.Mode.ENABLED
            buildSettingsMode = VersionedSettings.BuildSettingsMode.PREFER_CURRENT_SETTINGS
            rootExtId = Kotlin_KotlinEclipse_HttpsGithubComWpopielarskiKotlinEclipse.id
            showChanges = false
            settingsFormat = VersionedSettings.Format.KOTLIN
            storeSecureParamsOutsideOfVcs = true
        }
    }
})
