fun foo() {
    val a = A()
    a.f1(f<caret>2(1))
}

class A {
    fun f1(x: Int): Int = 1
}

fun f2(x: Int) = 2

fun main(args : Array<String>) {
    //Breakpoint!
    foo()
}

// LINE: 10