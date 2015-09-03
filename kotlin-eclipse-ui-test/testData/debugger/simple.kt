fun foo() {
    ba<caret>r()
}

fun bar() {
    val x = 1
}

fun main(args : Array<String>) {
    //Breakpoint!
    foo()
}

// LINE: 6
// TYPE: _DefaultPackage