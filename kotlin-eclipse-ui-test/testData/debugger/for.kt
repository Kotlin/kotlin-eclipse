fun foo() {
    val a = 1
    for (a in f<caret>1()) {
        f2()
    }
}

fun f1() = 1..2
fun f2() {}

fun main(args : Array<String>) {
    foo()
}

// LINE: 8