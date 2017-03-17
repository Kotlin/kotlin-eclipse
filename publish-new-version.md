#### How to publish new Kotlin Eclipse Version

 * Update TeamCity version prefix and reset counter for [Eclipse Build Configuration](https://teamcity.jetbrains.com/admin/editBuild.html?id=buildType:Kotlin_EclipsePlugin)
 * Update version in `maven-build/maven-update-version.launch` launch configuration and execute it
 * Update version in description of [kotlin-eclipse-feature](https://github.com/JetBrains/kotlin-eclipse/blob/master/kotlin-eclipse-feature/feature.xml)
 * Push the changes and wait for the [successful build](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_EclipsePlugin&branch_Kotlin=%3Cdefault%3E&tab=buildTypeStatusDiv)
 * *Check that version is correct!*
 * Pin the build and add `release` tag
 * Add release notes at [GitHub Releases](https://github.com/JetBrains/kotlin-eclipse/releases)
 * Upload artifacts to [Eclipse-Plugin package](https://bintray.com/jetbrains/kotlin/eclipse-plugin/view) at BinTray:
   * Download and unpack artifacts.zip from the [last build with release tag](https://teamcity.jetbrains.com/repository/downloadAll/Kotlin_EclipsePlugin/release.buildtag/artifacts.zip)
   * Unpack the file into some folder
   * Download [*pushToBintray.sh*](https://github.com/goodwinnk/bintray-publish-p2-updatesite) script that will help you in uploading files
   * Execute it (Git Bash (MinGW32) can be used on windows): 
     ```$ ./pushToBintray.sh BINTRAY_USER_NAME BINTRAY_REST_API_KEY jetbrains kotlin eclipse-plugin NEW_VERSION PATH_TO_UNPACKED_PLUGIN last PREVIOUS_VERSION```
 * **Test that you can install new version from Eclipse!**

