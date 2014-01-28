Kotlin for Eclipse
==============

##Building and Development

Eclipse Kepler (4.3.1) is a recomended way for building and developing kotlin-eclipse project so far. [Eclipse SDK](http://download.eclipse.org/eclipse/downloads/drops4/R-4.3.1-201309111000/) version could be a good choise for development because of bundled sources for eclipse core and JDT plugins.

In order to start development in Eclipse:
 - Import plugin projects from cloned repository into workspace 
 
        File->Import->Existing Projects into Workspace

 - Run laungh configuration for downloading Kotlin compiler. It will be used as bundled compiler in built plugin and as library during development 
 
        kotlin-bundled-compiler/Get Bundled Kotlin.launch -> Run As -> Get Bundled Kotlin

 - Run another instance of Eclipse with Kotlin plugin inside 
 
        kotlin-eclipse-ui -> Run As -> Eclipse Application

Build from console is also available (Note that Maven **3.0.5** is required):

    cd {repository}/kotlin-bundled-compiler
    ant -f get_bundled.xml  

    cd {repository}
    mvn install

