fun foo() {
    val a = A()
    f2(a f<caret>1 1)
}

class A {
    fun f1(i: Int) = 1
}

fun f2(i: Int) {}

fun main(args : Array<String>) {
    foo()
}

// LINE: 7