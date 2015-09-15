fun foo() {
    a + b + c + <caret>d
}

val a = 1

val b = 1
    get

val c: Int
    get() = 1

val d: Int
    get() {
        return 1
    }

fun main(args : Array<String>) {
    foo()
}

// LINE: 15