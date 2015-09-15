fun foo() {
   if (f<caret>1()) {
        f2()
    }
}

fun f1() = true
fun f2() {}

fun main(args : Array<String>) {
    foo()
}

// LINE: 7