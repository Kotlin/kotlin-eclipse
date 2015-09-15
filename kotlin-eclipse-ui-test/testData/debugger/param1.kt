fun foo() {
    f<caret>1(f2())
}

fun f1(i: Int) {}
fun f2() = 1


fun main(args : Array<String>) {
    foo()
}

// LINE: 5