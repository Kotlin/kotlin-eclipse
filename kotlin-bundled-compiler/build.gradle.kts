import com.intellij.buildsupport.dependencies.PackageListFromSimpleFile
import com.intellij.buildsupport.resolve.http.idea.IntellijIdeaArtifactsResolver
import com.intellij.buildsupport.resolve.tc.kotlin.KotlinCompilerTCArtifactsResolver
import com.intellij.buildsupport.utils.FileUtils

apply(plugin = "base")

// constants
val teamcityBaseUrl ="https://teamcity.jetbrains.com"
val ideaSdkUrl = "https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea"

// properties that might/should be modifiable
val kotlinCompilerTcBuildId: String = project.findProperty("kotlinCompilerTcBuildId") as String? ?: "2423158"
val kotlinCompilerVersion: String = project.findProperty("kotlinCompilerVersion") as String? ?: "1.3.50"
val kotlinxVersion: String = project.findProperty("kolinxVersion") as String? ?: "1.1.1"
val tcArtifactsPath: String = project.findProperty("tcArtifactsPath") as String? ?: ""
val ideaVersion: String = project.findProperty("ideaVersion") as String? ?: "183.5429.1"
val kotlinIdeaCompatibleVersionMinor: String = project.findProperty("kotlinIdeaCompatibleVersionMinor") as String? ?: "2018.3"
val ignoreSources: Boolean = project.hasProperty("ignoreSources")

//directories
val testDataDir = file("${projectDir.parentFile}/kotlin-eclipse-ui-test/common_testData")
//TODO later refactor to the proper project dir
val testModuleLibDir = file("${projectDir.parentFile}/kotlin-eclipse-ui-test/lib")
//TODO later refactor to the proper project dir

val downloadDirName = "downloads"

val teamCityWorkingDir = project.findProperty("teamcity.buildsupport.workingDir")
val libDir = if (teamCityWorkingDir != null) file("$teamCityWorkingDir/lib") else file("lib")

val localTCArtifacts: Boolean = tcArtifactsPath.isNotBlank()
val downloadDir = if(localTCArtifacts) file(tcArtifactsPath) else file("$libDir/$downloadDirName")

val tcArtifactsResolver = KotlinCompilerTCArtifactsResolver(teamcityBaseUrl,
        project.hasProperty("lastSuccessfulBuild"),
        kotlinCompilerTcBuildId,
        kotlinCompilerVersion,
        kotlinIdeaCompatibleVersionMinor)

val ideaArtifactsResolver = IntellijIdeaArtifactsResolver(ideaSdkUrl, ideaVersion)


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
        if (!localTCArtifacts) {
            tcArtifactsResolver.downloadTo(tcArtifactsResolver.KOTLIN_TEST_DATA_ZIP, locallyDownloadedTestDataFile)
        }

        copy {
            from(zipTree(locallyDownloadedTestDataFile))
            into(testDataDir)
        }

        locallyDownloadedTestDataFile.delete()
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
        if (!localTCArtifacts) {
            tcArtifactsResolver.downloadTo(tcArtifactsResolver.KOTLIN_PLUGIN_ZIP, locallyDownloadedCompilerFile)
        }

        copy {
            from(zipTree(locallyDownloadedCompilerFile))

            setIncludes(setOf("Kotlin/lib/kotlin-plugin.jar",
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
    destinationDir = libDir
    archiveName = "kotlin-converter.jar"
    include("org/jetbrains/kotlin/j2k/**")

    doLast {
        file("$libDir/kotlin-plugin.jar").delete()
    }
}

val downloadKotlinTCArtifacts by tasks.registering {
    doLast {
        tcArtifactsResolver.downloadTo(tcArtifactsResolver.KOTLIN_IDE_COMMON_JAR, file("$libDir/kotlin-ide-common.jar"))
        tcArtifactsResolver.downloadTo(tcArtifactsResolver.KOTLIN_FORMATTER_JAR, file("$libDir/kotlin-formatter.jar"))
    }
}

val downloadIntellijCoreAndExtractSelectedJars by tasks.registering {
    val locallyDownloadedIntellijCoreFile by extra { file("$downloadDir/intellij-core.zip") }

    doLast {
        ideaArtifactsResolver.downloadTo(ideaArtifactsResolver.INTELLIJ_CORE_ZIP, locallyDownloadedIntellijCoreFile)

        copy {
            from(zipTree(locallyDownloadedIntellijCoreFile))

            setIncludes(setOf("intellij-core.jar"))

            includeEmptyDirs = false

            into(libDir)
        }
    }
}

val downloadIdeaDistributionZipAndExtractSelectedJars by tasks.registering {
    val locallyDownloadedIdeaZipFile by extra { file("$downloadDir/ideaIC.zip") }
    val chosenJars by extra { setOf("openapi",
            "util",
            "idea",
            "trove4j",
            "platform-api",
            "platform-impl") }

    doLast {
        ideaArtifactsResolver.downloadTo(ideaArtifactsResolver.IDEA_IC_ZIP, locallyDownloadedIdeaZipFile)

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
        PackageListFromSimpleFile("referencedPackages.txt").pathsToInclude
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
    destinationDir = libDir
    archiveName = "ide-dependencies.jar"

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
        tcArtifactsResolver.downloadTo(tcArtifactsResolver.KOTLIN_COMPILER_SOURCES_JAR, locallyDownloadedKotlinCompilerSourcesFile)
        ideaArtifactsResolver.downloadTo(ideaArtifactsResolver.IDEA_IC_SOURCES_JAR, locallyDownloadedIdeaSourcesFile)
    }
}

val repackageIdeaAndKotlinCompilerSources by tasks.registering(Zip::class) {
    dependsOn(downloadIdeaAndKotlinCompilerSources)

    val locallyDownloadedKotlinCompilerSourcesFile: File by downloadIdeaAndKotlinCompilerSources.get().extra
    val locallyDownloadedIdeaSourcesFile: File by downloadIdeaAndKotlinCompilerSources.get().extra

    from(zipTree(locallyDownloadedKotlinCompilerSourcesFile))
    from(zipTree(locallyDownloadedIdeaSourcesFile))

    destinationDir = libDir
    archiveName = "kotlin-compiler-sources.jar"
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
                downloadKotlinTCArtifacts,
                downloadKotlinxLibraries)
    }

    if (!ignoreSources) {
        dependsOn(repackageIdeaAndKotlinCompilerSources)
    }
}

val getBundled by tasks.registering {
    dependsOn(downloadTestData, downloadTestFrameworkDependencies, downloadBundled)
}