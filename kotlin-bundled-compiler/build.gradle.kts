import com.intellij.buildsupport.dependencies.PackageListFromSimpleFile
import com.intellij.buildsupport.resolve.http.HttpArtifact
import com.intellij.buildsupport.resolve.http.HttpArtifactsResolver
import com.intellij.buildsupport.resolve.http.idea.IntellijIdeaArtifactsResolver
import com.intellij.buildsupport.resolve.tc.kotlin.KotlinCompilerTCArtifactsResolver
import com.intellij.buildsupport.utils.FileUtils

apply(plugin = "base")

// constants
val teamcityBaseUrl ="https://teamcity.jetbrains.com"
val ideaSdkUrl = "https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea"

// properties that might/should be modifiable

//val kotlinCompilerTcBuildId: String = project.findProperty("kotlinCompilerTcBuildId") as String? ?: "3546752"
val kotlinPluginUpdateId = project.findProperty("kotlinPluginUpdateId") as String? ?: "137462" // Kotlin Plugin 1.5.31 for Idea 2020.2

val kotlinCompilerVersion: String = project.findProperty("kotlinCompilerVersion") as String? ?: "1.5.31"
val kotlinxVersion: String = project.findProperty("kolinxVersion") as String? ?: "1.5.2"
val tcArtifactsPath: String = project.findProperty("tcArtifactsPath") as String? ?: ""
val ideaVersion: String = project.findProperty("ideaVersion") as String? ?: "203.8084.24" //Idea 2020.2
val kotlinIdeaCompatibleVersionMinor: String = project.findProperty("kotlinIdeaCompatibleVersionMinor") as String? ?: "2020.3"
val ignoreSources: Boolean = true//project.hasProperty("ignoreSources")

//directories
val testDataDir = file("${projectDir.parentFile}/kotlin-eclipse-ui-test/common_testData")
//TODO later refactor to the proper project dir
val testModuleLibDir = file("${projectDir.parentFile}/kotlin-eclipse-ui-test/lib")
//TODO later refactor to the proper project dir

val downloadDirName = "downloads$ideaVersion-$kotlinCompilerVersion"

val teamCityWorkingDir = project.findProperty("teamcity.buildsupport.workingDir")
val libDir = if (teamCityWorkingDir != null) file("$teamCityWorkingDir/lib") else file("lib")

val localTCArtifacts: Boolean = tcArtifactsPath.isNotBlank()
val downloadDir = if(localTCArtifacts) file(tcArtifactsPath) else file("$libDir/$downloadDirName")

/*val tcArtifactsResolver = KotlinCompilerTCArtifactsResolver(teamcityBaseUrl,
        project.hasProperty("lastSuccessfulBuild"),
        kotlinCompilerTcBuildId,
        kotlinCompilerVersion,
        kotlinIdeaCompatibleVersionMinor)*/

HttpArtifactsResolver.getProxyProps()["https.proxyHost"] = project.findProperty("https.proxyHost") ?: System.getProperty("https.proxyHost")
HttpArtifactsResolver.getProxyProps()["https.proxyPort"] = project.findProperty("https.proxyPort") ?: System.getProperty("https.proxyPort")
HttpArtifactsResolver.getProxyProps()["https.proxyUser"] = project.findProperty("https.proxyUser") ?: System.getProperty("https.proxyUser")
HttpArtifactsResolver.getProxyProps()["https.proxyPassword"] = project.findProperty("https.proxyPassword") ?: System.getProperty("https.proxyPassword")

val ideaArtifactsResolver = IntellijIdeaArtifactsResolver(ideaSdkUrl, ideaVersion)
val kotlinPluginArtifactsResolver = HttpArtifactsResolver("https://plugins.jetbrains.com")

val tempKotlinHttpArtifact = HttpArtifact("plugin/download?rel=true&updateId=$kotlinPluginUpdateId")

tasks.withType<Wrapper> {
    gradleVersion = "5.5.1"
}

val testFrameworkDependencies by configurations.creating
val kotlinxLibraries by configurations.creating

dependencies {
    testFrameworkDependencies("com.google.code.gson:gson:2.3.1")
    kotlinxLibraries("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion") { isTransitive = false }
    kotlinxLibraries("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxVersion") { isTransitive = false }
}

repositories {
    mavenCentral()
}

tasks.named<Delete>("clean") {
    doLast {
        FileUtils.cleanDir(testDataDir)
        FileUtils.cleanDir(testModuleLibDir)
        FileUtils.cleanDirExceptSubDirName(libDir, downloadDirName)
    }
}

val downloadTestData by tasks.registering {
    val locallyDownloadedTestDataFile by extra {
        if(localTCArtifacts){
            file("$tcArtifactsPath/kotlin-test-data.zip")
        } else {
            file("$testDataDir/kotlin-test-data.zip")
        }
    }

    doLast {
        //TODO can we get the test data from somewhere?
        if (!localTCArtifacts && !locallyDownloadedTestDataFile.exists()) {
            //tcArtifactsResolver.downloadTo(tcArtifactsResolver.KOTLIN_TEST_DATA_ZIP, locallyDownloadedTestDataFile)
        }

        /*copy {
            from(zipTree(locallyDownloadedTestDataFile))
            into(testDataDir)
        }*/
    }
}

val downloadTestFrameworkDependencies by tasks.registering(Copy::class) {
    from(testFrameworkDependencies)
    into(testModuleLibDir)
}

val downloadKotlinCompilerPluginAndExtractSelectedJars by tasks.registering {
    val locallyDownloadedCompilerFile by extra {
        file(downloadDir).listFiles()?.firstOrNull { it.name.startsWith("kotlin-plugin-") }
                ?: file("$downloadDir/kotlin-plugin.zip")
    }

    doLast {
        if (!localTCArtifacts && !locallyDownloadedCompilerFile.exists()) {
            kotlinPluginArtifactsResolver.downloadTo(tempKotlinHttpArtifact, locallyDownloadedCompilerFile)
            //tcArtifactsResolver.downloadTo(tcArtifactsResolver.KOTLIN_PLUGIN_ZIP, locallyDownloadedCompilerFile)
        }

        copy {
            from(zipTree(locallyDownloadedCompilerFile))

            setIncludes(setOf("Kotlin/lib/kotlin-plugin.jar",
                    "Kotlin/lib/ide-common.jar",
                    "Kotlin/lib/kotlin-core.jar",
                    "Kotlin/lib/kotlin-idea.jar",
                    "Kotlin/lib/kotlin-common.jar",
                    "Kotlin/lib/kotlin-j2k-old.jar",
                    "Kotlin/lib/kotlin-j2k-new.jar",
                    "Kotlin/lib/kotlin-j2k-idea.jar",
                    "Kotlin/lib/kotlin-j2k-services.jar",
                    "Kotlin/lib/kotlin-frontend-independent.jar",
                    "Kotlin/lib/kotlin-formatter.jar",
                    "Kotlin/kotlinc/lib/kotlin-compiler.jar",
                    "Kotlin/kotlinc/lib/kotlin-stdlib.jar",
                    "Kotlin/kotlinc/lib/kotlin-reflect.jar",
                    "Kotlin/kotlinc/lib/kotlin-script-runtime.jar",
                    "Kotlin/kotlinc/lib/kotlin-scripting-compiler.jar",
                    "Kotlin/kotlinc/lib/kotlin-scripting-common.jar",
                    "Kotlin/kotlinc/lib/kotlin-scripting-jvm.jar",
                    "Kotlin/kotlinc/lib/kotlin-scripting-compiler-impl.jar",
                    "Kotlin/kotlinc/lib/kotlin-jdk-annotations.jar",
                    "Kotlin/kotlinc/lib/kotlin-stdlib-sources.jar",
                    "Kotlin/kotlinc/lib/allopen-compiler-plugin.jar",
                    "Kotlin/kotlinc/lib/noarg-compiler-plugin.jar",
                    "Kotlin/kotlinc/lib/sam-with-receiver-compiler-plugin.jar",
                    "Kotlin/kotlinc/lib/annotations-13.0.jar"))

            includeEmptyDirs = false
            into(libDir)

            // flatten + rename
            eachFile {
                this.relativePath = RelativePath(true, this.name)
            }
        }
    }
}

val extractPackagesFromPlugin by tasks.registering(Jar::class) {
    dependsOn(downloadKotlinCompilerPluginAndExtractSelectedJars)

    from(zipTree("$libDir/kotlin-plugin.jar"))
    destinationDirectory.set(libDir)
    archiveFileName.set("kotlin-plugin-parts.jar")
    include("**")
    exclude("com/intellij/util/**")

    doLast {
        file("$libDir/kotlin-plugin.jar").delete()
    }
}

val downloadIntellijCoreAndExtractSelectedJars by tasks.registering {
    val locallyDownloadedIntellijCoreFile by extra { file("$downloadDir/intellij-core.zip") }

    doLast {
        if(!locallyDownloadedIntellijCoreFile.exists()) {
            ideaArtifactsResolver.downloadTo(ideaArtifactsResolver.INTELLIJ_CORE_ZIP, locallyDownloadedIntellijCoreFile)
        }
        copy {
            from(zipTree(locallyDownloadedIntellijCoreFile))

            setIncludes(setOf("intellij-core.jar", "intellij-core-analysis-deprecated.jar"))

            includeEmptyDirs = false

            into(libDir)
        }
    }
}

val downloadIdeaDistributionZipAndExtractSelectedJars by tasks.registering {
    val locallyDownloadedIdeaZipFile by extra { file("$downloadDir/ideaIC.zip") }
    val chosenJars by extra { setOf(//"openapi",
            "platform-util-ui",
            "util",
            "idea",
            "trove4j",
            "platform-api",
            "platform-impl") }

    doLast {
        if(!locallyDownloadedIdeaZipFile.exists()) {
            ideaArtifactsResolver.downloadTo(ideaArtifactsResolver.IDEA_IC_ZIP, locallyDownloadedIdeaZipFile)
        }
        copy {
            from(zipTree(locallyDownloadedIdeaZipFile))

            setIncludes(chosenJars.map { "lib/$it.jar" }.toSet())

            includeEmptyDirs = false

            into(libDir)

// flatten the files
            eachFile {
                this.relativePath = RelativePath(true, this.name)
            }
        }
    }
}

val extractSelectedFilesFromIdeaJars by tasks.registering {
    dependsOn(downloadIdeaDistributionZipAndExtractSelectedJars)

    val packages by extra {
        /*new PackageListFromManifest("META-INF/MANIFEST.MF"),*/
        PackageListFromSimpleFile(file("referencedPackages.txt").path).pathsToInclude
    }
    val extractDir by extra { file("$downloadDir/dependencies") }

    doLast {
        val chosenJars: Set<String> by downloadIdeaDistributionZipAndExtractSelectedJars.get().extra
        for (library in chosenJars) {
            copy {
                from(zipTree("$libDir/$library.jar"))
                setIncludes(packages)
                includeEmptyDirs = false
                into(extractDir)
            }
            file("$libDir/$library.jar").delete()
        }
    }
}

val createIdeDependenciesJar by tasks.registering(Jar::class) {
    dependsOn(extractSelectedFilesFromIdeaJars)

    val extractDir: File by extractSelectedFilesFromIdeaJars.get().extra

    from(extractDir)
    destinationDirectory.set(libDir)
    archiveFileName.set("ide-dependencies.jar")

    manifest {
        attributes(mapOf("Built-By" to "JetBrains",
                "Implementation-Vendor" to "JetBrains",
                "Implementation-Version" to "1.0",
                "Implementation-Title" to "ide-dependencies"))
    }

    doLast {
        extractDir.deleteRecursively()
    }
}

val downloadKotlinxLibraries by tasks.registering(Copy::class) {
    from(kotlinxLibraries)
    into(libDir)
    rename("(kotlinx-coroutines-\\w+)-.*", "$1.jar")
}

val downloadIdeaAndKotlinCompilerSources by tasks.registering {
    val locallyDownloadedKotlinCompilerSourcesFile by extra { file("$downloadDir/kotlin-compiler-sources.jar") }
    val locallyDownloadedIdeaSourcesFile by extra { file("$downloadDir/idea-sdk-sources.jar") }

    doLast {
        if(!locallyDownloadedKotlinCompilerSourcesFile.exists()) {
            //TODO can we get the sources from somewhere?
            //tcArtifactsResolver.downloadTo(tcArtifactsResolver.KOTLIN_COMPILER_SOURCES_JAR, locallyDownloadedKotlinCompilerSourcesFile)
        }
        if(!locallyDownloadedIdeaSourcesFile.exists()) {
            ideaArtifactsResolver.downloadTo(ideaArtifactsResolver.IDEA_IC_SOURCES_JAR, locallyDownloadedIdeaSourcesFile)
        }
    }
}

val repackageIdeaAndKotlinCompilerSources by tasks.registering(Zip::class) {
    dependsOn(downloadIdeaAndKotlinCompilerSources)

    val locallyDownloadedKotlinCompilerSourcesFile: File by downloadIdeaAndKotlinCompilerSources.get().extra
    val locallyDownloadedIdeaSourcesFile: File by downloadIdeaAndKotlinCompilerSources.get().extra

    from(zipTree(locallyDownloadedKotlinCompilerSourcesFile))
    from(zipTree(locallyDownloadedIdeaSourcesFile))

    destinationDirectory.set(libDir)
    archiveFileName.set("kotlin-compiler-sources.jar")
}

val downloadBundled by tasks.registering {
    if (localTCArtifacts) {
        dependsOn(downloadKotlinCompilerPluginAndExtractSelectedJars,
                extractPackagesFromPlugin,
                downloadIntellijCoreAndExtractSelectedJars,
                createIdeDependenciesJar,
                downloadKotlinxLibraries)
    } else {
        dependsOn(downloadKotlinCompilerPluginAndExtractSelectedJars,
                extractPackagesFromPlugin,
                downloadIntellijCoreAndExtractSelectedJars,
                createIdeDependenciesJar,
                downloadKotlinxLibraries)
    }

    if (!ignoreSources) {
        dependsOn(repackageIdeaAndKotlinCompilerSources)
    }
}

val getBundled by tasks.registering {
    dependsOn(downloadTestData, downloadTestFrameworkDependencies, downloadBundled)
}
