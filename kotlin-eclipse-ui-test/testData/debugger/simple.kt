fun foo() {
    ba<caret>r()
}

fun bar() {
    val x = 1
}

fun main(args : Array<String>) {
    foo()
}

// LINE: 6