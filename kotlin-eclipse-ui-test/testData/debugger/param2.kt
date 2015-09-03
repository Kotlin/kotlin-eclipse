fun foo() {
    f1(f<caret>2())
}

fun f1(i: Int) {}
fun f2() = 1


fun main(args : Array<String>) {
    //Breakpoint!
    foo()
}

// LINE: 6