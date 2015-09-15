fun foo() = b<caret>ar()

fun bar() = 1

fun main(args : Array<String>) {
    foo()
}

// LINE: 3