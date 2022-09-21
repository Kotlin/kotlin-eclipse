Kotlin for Eclipse
==============

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

Welcome to Kotlin for Eclipse project! Some handy links:

 * [Kotlin Site](http://kotlinlang.org/)
 * [Getting Started Guide](http://kotlinlang.org/docs/tutorials/getting-started-eclipse.html)
 * [Kotlin on Eclipse Marketplace](https://marketplace.eclipse.org/content/kotlin-plugin-eclipse)
 * Issue Tracker: [File New Issue](https://youtrack.jetbrains.com/newIssue?project=KE&clearDraft=true), [All Open Issues](https://youtrack.jetbrains.com/issues/KE?q=%23Unresolved)
 * [Kotlin Blog](http://blog.jetbrains.com/kotlin/)
 * [Forum](https://discuss.kotlinlang.org/)
 * [TeamCity CI build](https://teamcity.jetbrains.com/viewType.html?buildTypeId=Kotlin_EclipsePlugin)
 * [Follow Kotlin on Twitter](https://twitter.com/kotlin)

### Installation

To give it a try you will need a clean installation of Eclipse Neon or newer. The Kotlin plugin is available from the Eclipse Marketplace. The easiest way to install the Kotlin plugin is to **drag-and-drop this button into a running Eclipse window**:

<a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=2257536" class="drag" title="Drag to your running Eclipse workspace to install Kotlin Plugin for Eclipse"><img src="https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/components/drag-drop/installbutton.png" alt="Drag to your running Eclipse workspace to install Kotlin Plugin for Eclipse" /></a>

Alternatively, you can use *Help -> Eclipse Marketplace…* menu, or the following update site:

    https://dl.bintray.com/jetbrains/kotlin/eclipse-plugin/last/

### Building and Development

*Eclipse IDE for Eclipse Committers* is the recommended way to build and develop the `kotlin-eclipse` project. Eclipse [Oxygen 4.7](https://www.eclipse.org/downloads/packages/eclipse-ide-eclipse-committers/oxygenr) is used so far.

In order to start development in Eclipse:
 - Install the [AspectJ Eclipse plug-in for Eclipse 4.7](http://www.eclipse.org/ajdt/downloads/index.php). To install AJDT 2.2.4 use the following update site:

		http://download.eclipse.org/tools/ajdt/47/dev/update

 - Since Kotlin plugin contains code written in Kotlin itself, you will also need a Kotlin plugin to build the project in Eclipse. To install the Kotlin Eclipse plugin use the following update site:

 		https://teamcity.jetbrains.com/guestAuth/repository/download/Kotlin_EclipsePlugin/bootstrap.tcbuildtag/

 - Since Kotlin plugin uses weaving, you need to launch the project with weaving enabled. Installation of Equinox Weaving Launcher will add two additional launch configurations types for running plugin and for testing. To install the Equinox Weaving Launcher you can use the following update site: 

 		http://download.scala-ide.org/plugins/equinox-weaving-launcher/releases/site/

 - Import plugin projects from the cloned repository into your workspace 
 
        File -> Import -> Existing Projects into Workspace

 - Using the command line, run gradle build to download the Kotlin compiler. It will be used as a bundled compiler in built plugin and as a library during development.
 
       cd {repository}/kotlin-bundled-compiler
       ./gradlew clean getBundled
    or in Windows environment:
  
       cd {repository}\kotlin-bundled-compiler
       gradlew.bat clean getBundled

 - Run another instance of Eclipse with the Kotlin plugin inside 
 
        kotlin-eclipse-ui -> Run As -> Eclipse Weaving enabled Eclipse Application

Building from the command line is also available (Note that Maven **3.0.5** is required):

    cd {repository}
    mvn install

### Eclipse update sites

Latest stable release:

    https://dl.bintray.com/jetbrains/kotlin/eclipse-plugin/last/

Any previously released version (replace *:version* with the version number):

    https://dl.bintray.com/jetbrains/kotlin/eclipse-plugin/:version/

Nightly build:

    https://teamcity.jetbrains.com/guestAuth/repository/download/Kotlin_EclipsePlugin/.lastSuccessful/

### Kotlin Eclipse Plugin Developer Documentation

See basic developer documentation [here](https://github.com/JetBrains/kotlin-eclipse/blob/master/docs/dev-documentation.md)
