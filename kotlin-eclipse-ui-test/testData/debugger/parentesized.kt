fun foo() {
    (b<caret>ar())
}

fun bar() {}

fun main(args : Array<String>) {
    //Breakpoint!
    foo()
}

// LINE: 5