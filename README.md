Kotlin for Eclipse
==============

##Building and Development

Eclipse Luna SR2 (4.4.2) is the recomended way to build and develop the `kotlin-eclipse` project so far. We recommend the [Eclipse SDK](http://download.eclipse.org/eclipse/downloads/drops4/R-4.4.2-201502041700/) version because of bundled sources for Eclipse core and JDT plugins.

In order to start development in Eclipse:
 - Install the [AspectJ Eclipse plug-in for Eclipse 4.4](http://www.eclipse.org/ajdt/downloads/index.php). To install AJDT 2.2.4 use the following update site: 

 		http://download.eclipse.org/tools/ajdt/44/dev/update

 - Since Kotlin plugin contains code written in Kotlin itself, you will also need a Kotlin plugin to build the project in Eclipse. To install the Kotlin Eclipse plugin use the following update site:

 		https://teamcity.jetbrains.com/guestAuth/repository/download/Kotlin_EclipsePlugin/bootstrap.tcbuildtag/

 - Since Kotlin plugin uses weaving, you need to launch the project with weaving enabled. Installation of Equinox Weaving Launcher will add two additional launch configurations types for running plugin and for testing. To install the Equinox Weaving Launcher you can use the following update site: 

 		http://download.scala-ide.org/plugins/equinox-weaving-launcher/releases/site/

 - Import plugin projects from the cloned repository into your workspace 
 
        File -> Import -> Existing Projects into Workspace

 - Run the launch configuration to download the Kotlin compiler. It will be used as a bundled compiler in built plugin and as a library during development.
 
        kotlin-bundled-compiler/Get Bundled Kotlin.launch -> Run As -> Get Bundled Kotlin

 - Run another instance of Eclipse with the Kotlin plugin inside 
 
        kotlin-eclipse-ui -> Run As -> Eclipse Weaving enabled Eclipse Application

Building from the command line is also available (Note that Maven **3.0.5** is required):

    cd {repository}/kotlin-bundled-compiler
    ant -f get_bundled.xml  

    cd {repository}
    mvn install

