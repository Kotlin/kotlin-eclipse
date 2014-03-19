Kotlin for Eclipse
==============

##Building and Development

Eclipse Kepler (4.3.1) is the recomended way to build and develop on the kotlin-eclipse project so far. The [Eclipse SDK](http://download.eclipse.org/eclipse/downloads/drops4/R-4.3.1-201309111000/) version could also be a good choice for development because of bundled sources for eclipse core and JDT plugins.

In order to start development in Eclipse:
 - Import plugin projects from the cloned repository into your workspace 
 
        File->Import->Existing Projects into Workspace

 - Run launch configuration for downloading Kotlin compiler. It will be used as a bundled compiler in built plugin and as a library during development 
 
        kotlin-bundled-compiler/Get Bundled Kotlin.launch -> Run As -> Get Bundled Kotlin

 - Run another instance of Eclipse with the Kotlin plugin inside 
 
        kotlin-eclipse-ui -> Run As -> Eclipse Application

Building from the console is also available (Note that Maven **3.0.5** is required):

    cd {repository}/kotlin-bundled-compiler
    ant -f get_bundled.xml  

    cd {repository}
    mvn install

