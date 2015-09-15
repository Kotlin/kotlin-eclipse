fun foo() {
   do {
        f2()
    } while (f<caret>1())
}

fun f1() = true
fun f2() {}

fun main(args : Array<String>) {
    foo()
}

// LINE: 7