fun foo() {
    val a = A()
    f<caret>2(a.f1())
}

class A {
    fun f1() = 1
}

fun f2(i: Int) {}

fun main(args : Array<String>) {
    //Breakpoint!
    foo()
}

// LINE: 10