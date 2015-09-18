package navtest

import testpackage.SimpleClass

class InFileTarget {
    
}

//InFileTarget
//navtest/LibraryNavigation.kt:InFileTarget
fun navigateToTheSameFile() {
    val x: InFileTarget? = null
}

//SimpleClass
//testpackage/testfile.kt:SimpleClass
fun navigateToAnotherFile() {
    val x: SimpleClass? = null
}