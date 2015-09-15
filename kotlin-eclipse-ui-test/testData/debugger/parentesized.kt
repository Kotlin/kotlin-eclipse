fun foo() {
    (b<caret>ar())
}

fun bar() {}

fun main(args : Array<String>) {
    foo()
}

// LINE: 5